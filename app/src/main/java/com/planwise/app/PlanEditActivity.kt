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

class PlanEditActivity : AppCompatActivity() {

    private lateinit var goalInput: EditText
    private lateinit var deadlineInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var topicsInput: EditText
    private lateinit var roleInput: AutoCompleteTextView
    private lateinit var planContentInput: EditText
    private lateinit var updateButton: Button
    private lateinit var regenerateButton: Button
    private lateinit var cancelButton: Button
    private lateinit var loadingText: TextView
    
    private var planId: Long = -1
    private var existingPlan: Plan? = null
    private val BACKEND_URL = "https://planwise-backend-vcg7.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_edit)

        // Initialize views
        goalInput = findViewById(R.id.edit_goal_input)
        deadlineInput = findViewById(R.id.edit_deadline_input)
        hoursInput = findViewById(R.id.edit_hours_input)
        topicsInput = findViewById(R.id.edit_topics_input)
        roleInput = findViewById(R.id.edit_role_spinner)
        planContentInput = findViewById(R.id.edit_plan_content)
        updateButton = findViewById(R.id.edit_update_button)
        regenerateButton = findViewById(R.id.edit_regenerate_button)
        cancelButton = findViewById(R.id.edit_cancel_button)
        loadingText = findViewById(R.id.edit_loading_text)

        // Setup role dropdown
        val roles = arrayOf("Student", "Professional")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        roleInput.setAdapter(adapter)

        // Get plan ID from intent
        planId = intent.getLongExtra("plan_id", -1)
        if (planId == -1L) {
            Toast.makeText(this, "Plan not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load plan data
        loadPlan()

        // Button listeners
        updateButton.setOnClickListener { updatePlan() }
        regenerateButton.setOnClickListener { regeneratePlan() }
        cancelButton.setOnClickListener { finish() }
    }

    private fun loadPlan() {
        lifecycleScope.launch {
            existingPlan = withContext(Dispatchers.IO) {
                PlanDatabase.getDatabase(this@PlanEditActivity)
                    .planDao()
                    .getPlanById(planId)
            }

            existingPlan?.let { plan ->
                goalInput.setText(plan.goal)
                deadlineInput.setText(plan.deadline.toString())
                hoursInput.setText(plan.dailyHours.toString())
                topicsInput.setText(plan.topics)
                roleInput.setText(plan.role.capitalize(), false)
                planContentInput.setText(plan.planText)
            } ?: run {
                Toast.makeText(this@PlanEditActivity, "Plan not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updatePlan() {
        val updatedContent = planContentInput.text.toString().trim()
        if (updatedContent.isEmpty()) {
            Toast.makeText(this, "Plan content cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val goal = goalInput.text.toString().trim()
        if (goal.isEmpty()) {
            Toast.makeText(this, "Goal cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.visibility = android.view.View.VISIBLE
        loadingText.text = "💾 Updating plan..."
        updateButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val updatedPlan = Plan(
                    id = planId,
                    goal = goal,
                    deadline = deadlineInput.text.toString().toIntOrNull() ?: 0,
                    dailyHours = hoursInput.text.toString().toIntOrNull() ?: 0,
                    role = roleInput.text.toString().lowercase(),
                    topics = topicsInput.text.toString().trim(),
                    planText = updatedContent,
                    createdAt = existingPlan?.createdAt ?: System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@PlanEditActivity)
                        .planDao()
                        .updatePlan(updatedPlan)
                }

                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    updateButton.isEnabled = true
                    Toast.makeText(this@PlanEditActivity, "✅ Plan updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    updateButton.isEnabled = true
                    Toast.makeText(this@PlanEditActivity, "❌ Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun regeneratePlan() {
        val goal = goalInput.text.toString().trim()
        val deadlineStr = deadlineInput.text.toString().trim()
        val hoursStr = hoursInput.text.toString().trim()
        val topics = topicsInput.text.toString().trim()
        val role = roleInput.text.toString().lowercase()

        if (goal.isEmpty() || deadlineStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.visibility = android.view.View.VISIBLE
        loadingText.text = "🔄 Regenerating plan..."
        regenerateButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val newPlanText = callGeneratePlanAPI(goal, deadlineStr.toInt(), hoursStr.toInt(), role, topics)
                
                val updatedPlan = Plan(
                    id = planId,
                    goal = goal,
                    deadline = deadlineStr.toInt(),
                    dailyHours = hoursStr.toInt(),
                    role = role,
                    topics = topics,
                    planText = newPlanText,
                    createdAt = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@PlanEditActivity)
                        .planDao()
                        .updatePlan(updatedPlan)
                }

                withContext(Dispatchers.Main) {
                    planContentInput.setText(newPlanText)
                    loadingText.visibility = android.view.View.GONE
                    regenerateButton.isEnabled = true
                    Toast.makeText(this@PlanEditActivity, "✅ Plan regenerated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingText.visibility = android.view.View.GONE
                    regenerateButton.isEnabled = true
                    Toast.makeText(this@PlanEditActivity, "❌ Failed to regenerate: ${e.message}", Toast.LENGTH_SHORT).show()
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

    //override fun onBackPressed() {
        //super.onBackPressed()
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    //}

    override fun onBackPressed() {
        // Just finish the activity - no custom transition
        super.onBackPressed()
    }
}