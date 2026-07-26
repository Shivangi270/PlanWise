package com.planwise.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.Plan
import com.planwise.app.data.PlanDatabase
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
    private lateinit var mockGenerateButton: Button
    private lateinit var resultText: TextView
    private lateinit var reviewButton: Button
    private lateinit var loadingText: TextView
    private lateinit var savePlanButton: Button

    private val BACKEND_URL = "https://planwise-backend-vcg7.onrender.com"
    private var currentPlanText: String = ""
    private var currentGoal: String = ""
    private var currentDeadline: Int = 0
    private var currentHours: Int = 0
    private var currentRole: String = ""
    private var currentTopics: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        goalInput = findViewById(R.id.goal_input)
        deadlineInput = findViewById(R.id.deadline_input)
        hoursInput = findViewById(R.id.hours_input)
        topicsInput = findViewById(R.id.topics_input)
        roleInput = findViewById(R.id.role_spinner)
        generateButton = findViewById(R.id.generate_button)
        mockGenerateButton = findViewById(R.id.mock_generate_button)
        resultText = findViewById(R.id.result_text)
        reviewButton = findViewById(R.id.review_button)
        loadingText = findViewById(R.id.loading_text)
        savePlanButton = findViewById(R.id.save_plan_button)

        val roles = arrayOf("Student", "Professional")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleInput.setAdapter(adapter)
        roleInput.setText("Student", false)

        generateButton.setOnClickListener { generatePlan() }
        mockGenerateButton.setOnClickListener { generateMockPlan() }
        reviewButton.setOnClickListener { reviewPlan() }
        savePlanButton.setOnClickListener { savePlanToDatabase() }
        
        savePlanButton.visibility = android.view.View.GONE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun generateMockPlan() {
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

        loadingText.text = "🧪 Generating mock plan..."
        loadingText.visibility = android.view.View.VISIBLE
        generateButton.isEnabled = false
        mockGenerateButton.isEnabled = false
        resultText.text = ""
        reviewButton.visibility = android.view.View.GONE
        savePlanButton.visibility = android.view.View.GONE

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            currentPlanText = """
📋 MOCK PLAN: $currentGoal

⏰ Timeline: $currentDeadline days
📅 Daily Hours: $currentHours
👤 Role: ${currentRole.capitalize()}
📚 Topics: ${if (currentTopics.isNotEmpty()) currentTopics else "Not specified"}

📆 WEEK 1:
• Monday: Topic 1 - 2 hours
• Tuesday: Topic 2 - 2 hours
• Wednesday: Topic 3 - 2 hours
• Thursday: Topic 4 - 2 hours
• Friday: Topic 5 - 2 hours
• Saturday: Revision - 2 hours
• Sunday: Rest

📆 WEEK 2:
• Monday: Topic 6 - 2 hours
• Tuesday: Topic 7 - 2 hours
• Wednesday: Topic 8 - 2 hours
• Thursday: Topic 9 - 2 hours
• Friday: Topic 10 - 2 hours
• Saturday: Revision - 2 hours
• Sunday: Rest

💡 TIPS:
1. Start with the most difficult topic first
2. Take a 5-minute break every 25 minutes
3. Review what you learned at the end of each day

✨ This is a mock plan for testing purposes.
""".trimIndent()

            resultText.text = currentPlanText
            loadingText.visibility = android.view.View.GONE
            generateButton.isEnabled = true
            mockGenerateButton.isEnabled = true
            reviewButton.visibility = android.view.View.VISIBLE
            reviewButton.text = "🔍 Review My Plan"
            savePlanButton.visibility = android.view.View.VISIBLE
            Toast.makeText(this, "✅ Mock plan generated!", Toast.LENGTH_SHORT).show()
        }, 1500)
    }

    private fun generatePlan() {
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
        mockGenerateButton.isEnabled = false
        resultText.text = ""
        reviewButton.visibility = android.view.View.GONE
        savePlanButton.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val plan = callGeneratePlanAPI(currentGoal, currentDeadline, currentHours, currentRole, currentTopics)
                currentPlanText = plan
                withContext(Dispatchers.Main) {
                    resultText.text = plan
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                    mockGenerateButton.isEnabled = true
                    reviewButton.visibility = android.view.View.VISIBLE
                    reviewButton.text = "🔍 Review My Plan"
                    savePlanButton.visibility = android.view.View.VISIBLE
                    Toast.makeText(this@MainActivity, "✅ Plan generated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText.text = "❌ Error: ${e.message}"
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                    mockGenerateButton.isEnabled = true
                }
            }
        }
    }

    private fun reviewPlan() {
        if (currentPlanText.isEmpty() || currentPlanText.contains("Error")) {
            Toast.makeText(this, "Please generate a plan first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentGoal.isEmpty()) {
            Toast.makeText(this, "Please enter your goal first", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.text = "🤔 Reviewing your plan..."
        loadingText.visibility = android.view.View.VISIBLE
        reviewButton.isEnabled = false

        // Show mock review instantly for testing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val mockReview = """
🔍 PLAN REVIEW

✅ REALITY CHECK:
Your plan to complete "$currentGoal" in $currentDeadline days with $currentHours hours daily is ambitious but achievable!

💡 SUGGESTIONS:
1. Break down the topics into smaller chunks
2. Start each day with the most difficult topic
3. Include 10-minute breaks between study sessions
4. Review previous day's learning before starting new topics

🎯 OPTIMIZED APPROACH:
- Week 1-2: Foundation topics (2 hours)
- Week 3-4: Advanced topics (2 hours)
- Week 5-6: Revision and practice (2 hours)
- Week 7-8: Mock tests and weak areas (2 hours)

📊 RECOMMENDATION:
Consider increasing daily hours to 5 on weekends for better coverage.

✨ This is a mock review for testing purposes.
""".trimIndent()

            resultText.text = mockReview
            reviewButton.text = "🔄 Review Again"
            reviewButton.isEnabled = true
            loadingText.visibility = android.view.View.GONE
            Toast.makeText(this, "✅ Review complete!", Toast.LENGTH_SHORT).show()
        }, 1500)
    }

    private fun savePlanToDatabase() {
        if (currentPlanText.isEmpty()) {
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
                    planText = currentPlanText,
                    createdAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@MainActivity)
                        .planDao()
                        .insertPlan(plan)
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
                throw Exception("Server error $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    // Review API is now mocked, so we keep this function but don't use it
    private suspend fun callReviewPlanAPI(plan: String, goal: String): String = withContext(Dispatchers.IO) {
        // This function is no longer used - review is mocked
        return@withContext "Mock review"
    }
}