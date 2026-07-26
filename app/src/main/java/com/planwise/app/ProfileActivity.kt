package com.planwise.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var avatarText: TextView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var plansCountText: TextView
    private lateinit var completedCountText: TextView
    private lateinit var streakCountText: TextView

    private lateinit var darkModeCard: CardView
    private lateinit var notificationsCard: CardView
    private lateinit var aboutCard: CardView
    private lateinit var rateCard: CardView
    private lateinit var logoutCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize views
        avatarText = findViewById(R.id.profile_avatar_text)
        userNameText = findViewById(R.id.profile_user_name)
        userEmailText = findViewById(R.id.profile_user_email)
        plansCountText = findViewById(R.id.profile_plans_count)
        completedCountText = findViewById(R.id.profile_completed_count)
        streakCountText = findViewById(R.id.profile_streak_count)

        darkModeCard = findViewById(R.id.profile_dark_mode_card)
        notificationsCard = findViewById(R.id.profile_notifications_card)
        aboutCard = findViewById(R.id.profile_about_card)
        rateCard = findViewById(R.id.profile_rate_card)
        logoutCard = findViewById(R.id.profile_logout_card)

        // Set user info (placeholder)
        userNameText.text = "PlanWise User"
        userEmailText.text = "user@planwise.app"
        avatarText.text = "PW"

        // Load stats
        loadProfileStats()

        // Click listeners
        darkModeCard.setOnClickListener {
            Toast.makeText(this, "🌙 Dark mode coming soon!", Toast.LENGTH_SHORT).show()
        }

        notificationsCard.setOnClickListener {
            Toast.makeText(this, "🔔 Notifications coming soon!", Toast.LENGTH_SHORT).show()
        }

        aboutCard.setOnClickListener {
            showAboutDialog()
        }

        rateCard.setOnClickListener {
            Toast.makeText(this, "⭐ Rate PlanWise on Google Play", Toast.LENGTH_SHORT).show()
        }

        logoutCard.setOnClickListener {
            showLogoutDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileStats()
    }

    private fun loadProfileStats() {
        lifecycleScope.launch {
            val dao = PlanDatabase.getDatabase(this@ProfileActivity).planDao()
            
            dao.getAllPlans().collect { planList ->
                plansCountText.text = planList.size.toString()
                val completed = planList.filter { it.isCompleted }.size
                completedCountText.text = completed.toString()
                streakCountText.text = calculateStreak(planList).toString()
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

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About PlanWise")
            .setMessage("""
                PlanWise v1.0.0
                
                PlanWise is an AI-powered planning assistant that helps you create, manage, and achieve your goals.
                
                Features:
                • AI plan generation
                • Plan management (save, edit, delete)
                • Goal tracking
                • Streak tracking
                
                Made with ❤️ for students and professionals.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                Toast.makeText(this, "Logged out (feature coming soon)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}