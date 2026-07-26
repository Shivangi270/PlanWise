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
                planContentInput.setText(plan.planText) // Show the actual generated plan
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
                // Use mock generation for now (since backend may not be working)
                // You can replace this with real API call when backend is ready
                val newPlanText = generateMockPlan(goal, deadlineStr.toInt(), hoursStr.toInt(), role, topics)
                
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

    private fun generateMockPlan(goal: String, deadline: Int, dailyHours: Int, role: String, topics: String): String {
        return """
📋 MOCK PLAN: $goal

⏰ Timeline: $deadline days
📅 Daily Hours: $dailyHours
👤 Role: ${role.capitalize()}
📚 Topics: ${if (topics.isNotEmpty()) topics else "Not specified"}

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

✨ Plan regenerated on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
""".trimIndent()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}