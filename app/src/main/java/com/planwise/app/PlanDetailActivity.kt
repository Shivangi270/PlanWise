package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanDetailActivity : AppCompatActivity() {

    private lateinit var goalText: TextView
    private lateinit var detailsText: TextView
    private lateinit var planContentText: TextView
    private lateinit var dateText: TextView
    private lateinit var editButton: ImageView

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
    }

    override fun onResume() {
        super.onResume()
        if (planId != -1L) {
            loadPlan(planId)
        }
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
                planContentText.text = plan.planText
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(plan.createdAt))
                dateText.text = "📆 Created: $date"
            } else {
                goalText.text = "Plan not found"
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}