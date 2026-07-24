package com.planwise.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    private val BACKEND_URL = "https://planwise-backend-vcg7.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        goalInput = findViewById(R.id.goal_input)
        deadlineInput = findViewById(R.id.deadline_input)
        hoursInput = findViewById(R.id.hours_input)
        topicsInput = findViewById(R.id.topics_input)
        roleInput = findViewById(R.id.role_spinner)
        generateButton = findViewById(R.id.generate_button)
        resultText = findViewById(R.id.result_text)
        reviewButton = findViewById(R.id.review_button)
        loadingText = findViewById(R.id.loading_text)

        // Setup role dropdown
        val roles = arrayOf("Student", "Professional")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleInput.setAdapter(adapter)
        roleInput.setText("Student", false)

        generateButton.setOnClickListener {
            generatePlan()
        }

        reviewButton.setOnClickListener {
            reviewPlan()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun generatePlan() {
        val goal = goalInput.text.toString().trim()
        val deadline = deadlineInput.text.toString().trim()
        val hours = hoursInput.text.toString().trim()
        val topics = topicsInput.text.toString().trim()
        val role = roleInput.text.toString().lowercase()

        if (goal.isEmpty() || deadline.isEmpty() || hours.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.text = "⏳ Generating your plan..."
        loadingText.visibility = android.view.View.VISIBLE
        generateButton.isEnabled = false
        resultText.text = ""
        reviewButton.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val plan = callGeneratePlanAPI(goal, deadline.toInt(), hours.toInt(), role, topics)
                withContext(Dispatchers.Main) {
                    resultText.text = plan
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                    reviewButton.visibility = android.view.View.VISIBLE
                    reviewButton.text = "🔍 Review My Plan"
                    Toast.makeText(this@MainActivity, "✅ Plan generated!", Toast.LENGTH_SHORT).show()
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
        val currentPlan = resultText.text.toString()
        if (currentPlan.isEmpty() || currentPlan.contains("Click 'Generate Plan'") || currentPlan.contains("Error")) {
            Toast.makeText(this, "Please generate a valid plan first", Toast.LENGTH_SHORT).show()
            return
        }

        val goal = goalInput.text.toString().trim()
        if (goal.isEmpty()) {
            Toast.makeText(this, "Please enter your goal first", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.text = "🤔 Reviewing your plan..."
        loadingText.visibility = android.view.View.VISIBLE
        reviewButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val review = callReviewPlanAPI(currentPlan, goal)
                withContext(Dispatchers.Main) {
                    resultText.text = "🔍 PLAN REVIEW\n\n$review"
                    reviewButton.text = "🔄 Review Again"
                    reviewButton.isEnabled = true
                    loadingText.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "✅ Review complete!", Toast.LENGTH_SHORT).show()
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

    private suspend fun callGeneratePlanAPI(
        goal: String,
        deadline: Int,
        dailyHours: Int,
        role: String,
        topics: String
    ): String = withContext(Dispatchers.IO) {
        val url = URL("$BACKEND_URL/generate-plan")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

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
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            }

            if (responseCode in 200..299) {
                val jsonResponse = JSONObject(response)
                jsonResponse.getString("plan")
            } else {
                throw Exception("Server error $responseCode: $response")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun callReviewPlanAPI(plan: String, goal: String): String = withContext(Dispatchers.IO) {
        val url = URL("$BACKEND_URL/review-plan")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            val jsonBody = JSONObject().apply {
                put("plan", plan)
                put("goal", goal)
            }

            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            }

            if (responseCode in 200..299) {
                val jsonResponse = JSONObject(response)
                jsonResponse.getString("review")
            } else {
                throw Exception("Server error $responseCode: $response")
            }
        } finally {
            connection.disconnect()
        }
    }
}