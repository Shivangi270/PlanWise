package com.craftycode.planwise

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.craftycode.planwise.data.Plan
import com.craftycode.planwise.data.PlanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var goalInput: EditText
    private lateinit var deadlineInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var topicsInput: EditText
    private lateinit var roleInput: AutoCompleteTextView
    private lateinit var generateButton: Button
    private lateinit var resultText: TextView
    private lateinit var reviewButton: Button
    private lateinit var loadingText: TextView
    private lateinit var savePlanButton: Button
    private lateinit var limitWarningText: TextView

    private val BACKEND_URL = "https://planwise-backend-vcg7.onrender.com"
    private var currentPlanText: String = ""
    private var currentGoal: String = ""
    private var currentDeadline: Int = 0
    private var currentHours: Int = 0
    private var currentRole: String = ""
    private var currentTopics: String = ""

    private lateinit var sharedPrefs: SharedPreferences
    private val PREFS_NAME = "PlanWisePrefs"
    private val KEY_LAST_GEN_DATE = "last_gen_date"
    private val KEY_GEN_COUNT = "gen_count"
    private val DAILY_LIMIT = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        goalInput = findViewById(R.id.goal_input)
        deadlineInput = findViewById(R.id.deadline_input)
        hoursInput = findViewById(R.id.hours_input)
        topicsInput = findViewById(R.id.topics_input)
        roleInput = findViewById(R.id.role_spinner)
        generateButton = findViewById(R.id.generate_button)
        resultText = findViewById(R.id.result_text)
        reviewButton = findViewById(R.id.review_button)
        loadingText = findViewById(R.id.loading_text)
        savePlanButton = findViewById(R.id.save_plan_button)
        limitWarningText = findViewById(R.id.limit_warning_text)

        val roles = arrayOf("Student", "Professional")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleInput.setAdapter(adapter)
        roleInput.setText("Student", false)

        generateButton.setOnClickListener { generatePlan() }
        reviewButton.setOnClickListener { reviewPlan() }
        savePlanButton.setOnClickListener { savePlanToDatabase() }

        savePlanButton.visibility = android.view.View.GONE
        updateLimitStatus()
    }

    // NOTE: onBackPressed() is intentionally NOT overridden.
    // Android's default back navigation is used to prevent crashes.

    private fun markdownToHtml(markdown: String): String {
        val extensions = listOf(TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        val html = renderer.render(parser.parse(markdown))
        return wrapTablesForMobile(html)
    }

    private fun wrapTablesForMobile(html: String): String {
        return html
            .replace(
                "<table>",
                "<div style='overflow-x:auto;'><table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse; font-size:12px;'>"
            )
            .replace("</table>", "</table></div>")
    }

    private fun displayPlan(markdown: String) {
        val html = markdownToHtml(markdown)
        resultText.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun canGeneratePlan(): Boolean {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val todayKey = "$year-$today"
        val lastGenDate = sharedPrefs.getString(KEY_LAST_GEN_DATE, "")
        val genCount = sharedPrefs.getInt(KEY_GEN_COUNT, 0)
        if (lastGenDate != todayKey) {
            sharedPrefs.edit().apply {
                putString(KEY_LAST_GEN_DATE, todayKey)
                putInt(KEY_GEN_COUNT, 0)
                apply()
            }
            return true
        }
        return genCount < DAILY_LIMIT
    }

    private fun incrementGenerationCount() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val todayKey = "$year-$today"
        val currentCount = sharedPrefs.getInt(KEY_GEN_COUNT, 0)
        sharedPrefs.edit().apply {
            putString(KEY_LAST_GEN_DATE, todayKey)
            putInt(KEY_GEN_COUNT, currentCount + 1)
            apply()
        }
        updateLimitStatus()
    }

    private fun updateLimitStatus() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val todayKey = "$year-$today"
        val lastGenDate = sharedPrefs.getString(KEY_LAST_GEN_DATE, "")
        val genCount = if (lastGenDate == todayKey) sharedPrefs.getInt(KEY_GEN_COUNT, 0) else 0
        val remaining = DAILY_LIMIT - genCount
        if (remaining > 0) {
            limitWarningText.text = "📊 $remaining of $DAILY_LIMIT plan generations remaining today"
            limitWarningText.setTextColor(getColor(android.R.color.holo_green_dark))
            generateButton.isEnabled = true
        } else {
            limitWarningText.text = "⚠️ Daily limit reached! Come back tomorrow."
            limitWarningText.setTextColor(getColor(android.R.color.holo_red_dark))
            generateButton.isEnabled = false
        }
    }

    private fun generatePlan() {
        if (!canGeneratePlan()) {
            Toast.makeText(this, "Daily plan limit reached! Come back tomorrow.", Toast.LENGTH_LONG).show()
            updateLimitStatus()
            return
        }

        currentGoal = goalInput.text.toString().trim()
        val deadlineStr = deadlineInput.text.toString().trim()
        val hoursStr = hoursInput.text.toString().trim()
        currentTopics = topicsInput.text.toString().trim()
        currentRole = roleInput.text.toString().lowercase()

        if (currentGoal.isEmpty() || deadlineStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        currentDeadline = deadlineStr.toInt()
        currentHours = hoursStr.toInt()

        loadingText.text = "⏳ Generating your plan..."
        loadingText.visibility = android.view.View.VISIBLE
        generateButton.isEnabled = false
        resultText.text = ""
        reviewButton.visibility = android.view.View.GONE
        savePlanButton.visibility = android.view.View.GONE
        currentPlanText = ""

        lifecycleScope.launch {
            try {
                var firstChunkReceived = false

                streamGeneratePlanAPI(
                    currentGoal, currentDeadline, currentHours, currentRole, currentTopics,
                    onFirstChunk = {
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            runOnUiThread {
                                loadingText.visibility = android.view.View.GONE
                            }
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                    reviewButton.visibility = android.view.View.VISIBLE
                    reviewButton.text = "🔍 Review My Plan"
                    savePlanButton.visibility = android.view.View.VISIBLE
                    Toast.makeText(this@MainActivity, "✅ Plan generated!", Toast.LENGTH_SHORT).show()
                    updateLimitStatus()
                }
                incrementGenerationCount()
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    resultText.text = "⏱️ Request timed out. Please try again."
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                }
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    resultText.text = "🌐 Network timeout. Please check your connection."
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText.text = "❌ Error: ${e.message}"
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                }
            }
        }
    }

        private fun reviewPlan() {
        // Prevent review while plan is still generating
        if (loadingText.visibility == android.view.View.VISIBLE) {
            Toast.makeText(this, "Please wait for the plan to finish generating", Toast.LENGTH_SHORT).show()
            return
        }

        // Use currentPlanText if available, otherwise fall back to what's displayed
        val planText = if (currentPlanText.isNotEmpty()) {
            currentPlanText
        } else {
            resultText.text.toString()
        }

        if (planText.isEmpty() ||
            planText.contains("Click 'Generate Plan'") ||
            planText.contains("Error") ||
            planText.contains("timeout") ||
            planText.contains("Generating your plan") ||
            planText.contains("⚠️ The plan may be incomplete")
        ) {
            Toast.makeText(this, "Please generate a valid plan first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentGoal.isEmpty()) {
            Toast.makeText(this, "Please enter your goal first", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.text = "🤔 Reviewing your plan..."
        loadingText.visibility = android.view.View.VISIBLE
        reviewButton.isEnabled = false
        resultText.text = ""

        lifecycleScope.launch {
            try {
                streamReviewPlanAPI(planText, currentGoal)
                withContext(Dispatchers.Main) {
                    reviewButton.text = "🔄 Review Again"
                    reviewButton.isEnabled = true
                    loadingText.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "✅ Review complete!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    resultText.text = "⏱️ Review timed out. Please try again."
                    reviewButton.isEnabled = true
                    loadingText.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText.text = "❌ Review failed: ${e.message}"
                    reviewButton.isEnabled = true
                    loadingText.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun savePlanToDatabase() {
        val planText = if (currentPlanText.isNotEmpty()) currentPlanText else resultText.text.toString()

        if (planText.isEmpty()) {
            Toast.makeText(this, "No plan to save", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.text = "💾 Saving plan..."
        loadingText.visibility = android.view.View.VISIBLE
        savePlanButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val plan = Plan(
                    goal = currentGoal,
                    deadline = currentDeadline,
                    dailyHours = currentHours,
                    role = currentRole,
                    topics = currentTopics,
                    planText = planText,
                    createdAt = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@MainActivity).planDao().insertPlan(plan)
                }
                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    savePlanButton.isEnabled = true
                    savePlanButton.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "✅ Plan saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    savePlanButton.isEnabled = true
                    Toast.makeText(this@MainActivity, "❌ Failed to save plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun streamGeneratePlanAPI(
        goal: String,
        deadline: Int,
        dailyHours: Int,
        role: String,
        topics: String,
        onFirstChunk: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = URL("$BACKEND_URL/generate-plan")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "text/plain")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 180000  // 3 minutes

            val jsonBody = JSONObject().apply {
                put("goal", goal)
                put("deadline", deadline)
                put("daily_hours", dailyHours)
                put("role", role)
                put("topics", topics)
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("Server error $responseCode: $error")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val mainHandler = Handler(Looper.getMainLooper())
            val buffer = StringBuilder()
            var lastUpdate = 0L
            var isFirstLine = true

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (isFirstLine) {
                    isFirstLine = false
                    mainHandler.post { onFirstChunk() }
                }
                buffer.append(line).append("\n")
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 150) {
                    val snapshot = buffer.toString()
                    mainHandler.post { displayPlan(snapshot) }
                    lastUpdate = now
                }
            }
            reader.close()

            currentPlanText = buffer.toString().trim()
            mainHandler.post { displayPlan(currentPlanText) }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun streamReviewPlanAPI(plan: String, goal: String) = withContext(Dispatchers.IO) {
        val url = URL("$BACKEND_URL/review-plan")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "text/plain")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 180000

            val jsonBody = JSONObject().apply {
                put("plan", plan)
                put("goal", goal)
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("Server error $responseCode: $error")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val mainHandler = Handler(Looper.getMainLooper())
            val buffer = StringBuilder()
            var lastUpdate = 0L

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line).append("\n")
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 150) {
                    val snapshot = "🔍 PLAN REVIEW\n\n" + buffer.toString()
                    mainHandler.post { displayPlan(snapshot) }
                    lastUpdate = now
                }
            }
            reader.close()

            val finalText = "🔍 PLAN REVIEW\n\n" + buffer.toString()
            currentPlanText = buffer.toString()
            mainHandler.post { displayPlan(finalText) }
        } finally {
            connection.disconnect()
        }
    }
}