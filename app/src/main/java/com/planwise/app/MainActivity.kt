package com.planwise.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var goalInput: EditText
    private lateinit var deadlineInput: EditText
    private lateinit var hoursInput: EditText
    private lateinit var generateButton: Button
    private lateinit var resultText: TextView
    private lateinit var reviewButton: Button
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        goalInput = findViewById(R.id.goal_input)
        deadlineInput = findViewById(R.id.deadline_input)
        hoursInput = findViewById(R.id.hours_input)
        generateButton = findViewById(R.id.generate_button)
        resultText = findViewById(R.id.result_text)
        reviewButton = findViewById(R.id.review_button)
        loadingText = findViewById(R.id.loading_text)

        generateButton.setOnClickListener {
            generatePlan()
        }

        reviewButton.setOnClickListener {
            reviewPlan()
        }
    }

    private fun generatePlan() {
        val goal = goalInput.text.toString().trim()
        val deadline = deadlineInput.text.toString().trim()
        val hours = hoursInput.text.toString().trim()

        if (goal.isEmpty() || deadline.isEmpty() || hours.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        loadingText.visibility = android.view.View.VISIBLE
        generateButton.isEnabled = false
        resultText.text = ""

        lifecycleScope.launch {
            try {
                // TODO: Replace with actual API call to your backend
                val plan = generateFakePlan(goal, deadline, hours)
                withContext(Dispatchers.Main) {
                    resultText.text = plan
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                    reviewButton.visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText.text = "Error: ${e.message}"
                    loadingText.visibility = android.view.View.GONE
                    generateButton.isEnabled = true
                }
            }
        }
    }

    private fun reviewPlan() {
        Toast.makeText(this, "Plan review coming soon! 🚀", Toast.LENGTH_SHORT).show()
        // TODO: Implement plan review with AI
    }

    // Temporary placeholder until backend is ready
    private fun generateFakePlan(goal: String, deadline: String, hours: String): String {
        return """
            📋 YOUR PLAN: $goal
            
            ⏰ Timeline: $deadline days
            📅 Daily Hours: $hours
            
            📆 WEEK 1:
            • Monday: Topic 1 - 2 hours
            • Tuesday: Topic 2 - 2 hours
            • Wednesday: Topic 3 - 2 hours
            • Thursday: Topic 4 - 2 hours
            • Friday: Topic 5 - 2 hours
            • Saturday: Revision - 2 hours
            • Sunday: Rest
            
            💡 TIPS:
            1. Start with the most difficult topic first
            2. Take a 5-minute break every 25 minutes
            3. Review what you learned at the end of each day
            
            ✨ Ask me to "Review My Plan" for suggestions!
        """.trimIndent()
    }
}