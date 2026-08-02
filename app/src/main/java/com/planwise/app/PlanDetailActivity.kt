package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.PlanDatabase
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanDetailActivity : AppCompatActivity() {

    private lateinit var goalText: TextView
    private lateinit var detailsText: TextView
    private lateinit var planContentText: TextView
    private lateinit var dateText: TextView
    private lateinit var editButton: ImageView
    private lateinit var completeButton: Button

    private var planId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_detail)

        goalText = findViewById(R.id.detail_goal_text)
        detailsText = findViewById(R.id.detail_details_text)
        planContentText = findViewById(R.id.detail_plan_content)
        dateText = findViewById(R.id.detail_date_text)
        editButton = findViewById(R.id.detail_edit_button)
        completeButton = findViewById(R.id.detail_complete_button)

        planId = intent.getLongExtra("plan_id", -1)
        if (planId != -1L) {
            loadPlan(planId)
        } else {
            goalText.text = "Plan not found"
        }

        editButton.setOnClickListener {
            val intent = Intent(this, PlanEditActivity::class.java)
            intent.putExtra("plan_id", planId)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        completeButton.setOnClickListener {
            toggleCompletion()
        }
    }

    override fun onResume() {
        super.onResume()
        if (planId != -1L) {
            loadPlan(planId)
        }
    }

    private fun markdownToHtml(markdown: String): String {
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    private fun loadPlan(planId: Long) {
        lifecycleScope.launch {
            val plan = withContext(Dispatchers.IO) {
                PlanDatabase.getDatabase(this@PlanDetailActivity)
                    .planDao()
                    .getPlanById(planId)
            }

            if (plan != null) {
                goalText.text = "🎯 ${plan.goal}"
                detailsText.text = "📅 ${plan.deadline} days • ⏰ ${plan.dailyHours} hrs/day • 👤 ${plan.role.capitalize()}"
                
                // Render markdown with CommonMark
                val html = markdownToHtml(plan.planText)
                planContentText.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
                
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(plan.createdAt))
                dateText.text = "📆 Created: $date"
                
                if (plan.isCompleted) {
                    completeButton.text = "✅ Completed!"
                    completeButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
                    completeButton.isEnabled = false
                } else {
                    completeButton.text = "🎯 Mark as Completed"
                    completeButton.setBackgroundColor(getColor(android.R.color.holo_blue_light))
                    completeButton.isEnabled = true
                }
            } else {
                goalText.text = "Plan not found"
            }
        }
    }

    private fun toggleCompletion() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@PlanDetailActivity)
                        .planDao()
                        .togglePlanCompletion(planId, true)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlanDetailActivity, "🎉 Plan marked as completed!", Toast.LENGTH_SHORT).show()
                    loadPlan(planId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlanDetailActivity, "❌ Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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