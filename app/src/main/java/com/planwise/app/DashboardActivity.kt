package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var createPlanButton: CardView
    private lateinit var viewPlansCard: CardView
    private lateinit var plansCount: TextView
    private lateinit var goalsCount: TextView
    private lateinit var streakCount: TextView
    private lateinit var recentPlansContainer: LinearLayout
    private lateinit var profileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        welcomeText = findViewById(R.id.welcome_text)
        createPlanButton = findViewById(R.id.create_plan_button)
        viewPlansCard = findViewById(R.id.view_plans_card)
        plansCount = findViewById(R.id.plans_count)
        goalsCount = findViewById(R.id.goals_count)
        streakCount = findViewById(R.id.streak_count)
        recentPlansContainer = findViewById(R.id.recent_plans_container)
        profileIcon = findViewById(R.id.profile_icon)

        // Set welcome message
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning! 👋"
            in 12..16 -> "Good Afternoon! 🌤️"
            in 17..20 -> "Good Evening! 🌅"
            else -> "Good Night! 🌙"
        }
        welcomeText.text = greeting

        // Set click listeners with FLAG_ACTIVITY_CLEAR_TOP to prevent duplicate activities
        createPlanButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        viewPlansCard.setOnClickListener {
            val intent = Intent(this, PlanListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    override fun onBackPressed() {
        // If this is the root activity, just move to background
        if (isTaskRoot) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val dao = PlanDatabase.getDatabase(this@DashboardActivity).planDao()
            
            dao.getAllPlans().collect { planList ->
                plansCount.text = planList.size.toString()
                val completed = planList.filter { it.isCompleted }.size
                goalsCount.text = completed.toString()
                streakCount.text = calculateStreak(planList).toString()

                recentPlansContainer.removeAllViews()
                
                if (planList.isEmpty()) {
                    val emptyView = layoutInflater.inflate(R.layout.item_empty_recent, recentPlansContainer, false)
                    recentPlansContainer.addView(emptyView)
                } else {
                    val recentPlans = planList.take(3)
                    for (plan in recentPlans) {
                        val planView = layoutInflater.inflate(R.layout.item_recent_plan, recentPlansContainer, false)
                        val goalText = planView.findViewById<TextView>(R.id.recent_plan_goal)
                        val dateText = planView.findViewById<TextView>(R.id.recent_plan_date)
                        
                        goalText.text = plan.goal
                        val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(plan.createdAt))
                        dateText.text = date
                        
                        planView.setOnClickListener {
                            val intent = Intent(this@DashboardActivity, PlanDetailActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    private fun calculateStreak(plans: List<com.planwise.app.data.Plan>): Int {
        if (plans.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = plans.map { 
            dateFormat.format(Date(it.createdAt))
        }.distinct().sorted()
        
        if (dates.isEmpty()) return 0
        
        val today = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        
        val hasToday = dates.contains(today)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)
        val hasYesterday = dates.contains(yesterday)
        
        if (!hasToday && !hasYesterday) {
            return 0
        }
        
        if (!hasToday && hasYesterday) {
            var streak = 0
            var currentDate = dateFormat.parse(yesterday) ?: Date()
            while (true) {
                val dateStr = dateFormat.format(currentDate)
                if (dates.contains(dateStr)) {
                    streak++
                    val cal = Calendar.getInstance()
                    cal.time = currentDate
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    currentDate = cal.time
                } else {
                    break
                }
            }
            return streak
        }
        
        var streak = 0
        var currentDate = dateFormat.parse(today) ?: Date()
        while (true) {
            val dateStr = dateFormat.format(currentDate)
            if (dates.contains(dateStr)) {
                streak++
                val cal = Calendar.getInstance()
                cal.time = currentDate
                cal.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = cal.time
            } else {
                break
            }
        }
        return streak
    }
}