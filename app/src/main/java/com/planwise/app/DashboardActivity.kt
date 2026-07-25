package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var createPlanButton: CardView
    private lateinit var viewPlansCard: CardView
    private lateinit var emptyCreateButton: Button
    private lateinit var plansCount: TextView
    private lateinit var goalsCount: TextView
    private lateinit var streakCount: TextView
    private lateinit var recentPlansContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        welcomeText = findViewById(R.id.welcome_text)
        createPlanButton = findViewById(R.id.create_plan_button)
        viewPlansCard = findViewById(R.id.view_plans_card)
        emptyCreateButton = findViewById(R.id.empty_create_button)
        plansCount = findViewById(R.id.plans_count)
        goalsCount = findViewById(R.id.goals_count)
        streakCount = findViewById(R.id.streak_count)
        recentPlansContainer = findViewById(R.id.recent_plans_container)

        // Set welcome message with time-based greeting
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning! 👋"
            in 12..16 -> "Good Afternoon! 🌤️"
            in 17..20 -> "Good Evening! 🌅"
            else -> "Good Night! 🌙"
        }
        welcomeText.text = "$greeting\nPlan Smarter, Achieve More"

        // Set placeholder stats
        plansCount.text = "0"
        goalsCount.text = "0"
        streakCount.text = "0"

        // Create Plan button with smooth transition
        createPlanButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // View Plans with smooth transition
        viewPlansCard.setOnClickListener {
            val intent = Intent(this, PlanListActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Empty state create button
        emptyCreateButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Load recent plans and stats
        loadRecentPlans()
    }

    override fun onResume() {
        super.onResume()
        loadRecentPlans()
    }

    private fun loadRecentPlans() {
        lifecycleScope.launch {
            val plans = PlanDatabase.getDatabase(this@DashboardActivity)
                .planDao()
                .getAllPlans()
                .collect { planList ->
                    // Update stats
                    plansCount.text = planList.size.toString()
                    
                    // Update recent plans
                    recentPlansContainer.removeAllViews()
                    
                    if (planList.isEmpty()) {
                        // Show empty state
                        val emptyView = layoutInflater.inflate(R.layout.item_empty_recent, recentPlansContainer, false)
                        recentPlansContainer.addView(emptyView)
                    } else {
                        // Show up to 3 most recent plans
                        val recentPlans = planList.take(3)
                        for (plan in recentPlans) {
                            val planView = layoutInflater.inflate(R.layout.item_recent_plan, recentPlansContainer, false)
                            val goalText = planView.findViewById<TextView>(R.id.recent_plan_goal)
                            val dateText = planView.findViewById<TextView>(R.id.recent_plan_date)
                            
                            goalText.text = plan.goal
                            val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(plan.createdAt))
                            dateText.text = date
                            
                            planView.setOnClickListener {
                                val intent = Intent(this@DashboardActivity, PlanDetailActivity::class.java)
                                intent.putExtra("plan_id", plan.id)
                                startActivity(intent)
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                            
                            recentPlansContainer.addView(planView)
                        }
                    }
                }
        }
    }
}