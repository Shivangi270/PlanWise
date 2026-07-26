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

class PlanEditActivity : AppCompatActivity() {

    private lateinit var goalInput: EditText
    private lateinit var deadlineInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var topicsInput: EditText
    private lateinit var roleInput: AutoCompleteTextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    private var planId: Long = -1
    private var existingPlan: Plan? = null

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
        saveButton = findViewById(R.id.edit_save_button)
        cancelButton = findViewById(R.id.edit_cancel_button)

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
        saveButton.setOnClickListener { savePlan() }
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
            } ?: run {
                Toast.makeText(this@PlanEditActivity, "Plan not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun savePlan() {
        val goal = goalInput.text.toString().trim()
        val deadlineStr = deadlineInput.text.toString().trim()
        val hoursStr = hoursInput.text.toString().trim()
        val topics = topicsInput.text.toString().trim()
        val role = roleInput.text.toString().lowercase()

        if (goal.isEmpty() || deadlineStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val deadline = deadlineStr.toInt()
        val dailyHours = hoursStr.toInt()

        lifecycleScope.launch {
            try {
                val updatedPlan = Plan(
                    id = planId,
                    goal = goal,
                    deadline = deadline,
                    dailyHours = dailyHours,
                    role = role,
                    topics = topics,
                    planText = existingPlan?.planText ?: "",
                    createdAt = existingPlan?.createdAt ?: System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    // Update in database
                    PlanDatabase.getDatabase(this@PlanEditActivity)
                        .planDao()
                        .updatePlan(updatedPlan)
                }

                Toast.makeText(this@PlanEditActivity, "✅ Plan updated!", Toast.LENGTH_SHORT).show()
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Exception) {
                Toast.makeText(this@PlanEditActivity, "❌ Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}