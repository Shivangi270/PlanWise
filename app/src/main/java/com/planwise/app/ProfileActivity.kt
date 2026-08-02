package com.planwise.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var feedbackCard: CardView
    private lateinit var aboutCard: CardView
    private lateinit var rateCard: CardView
    private lateinit var clearPlansCard: CardView  // Changed from logoutCard

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
        feedbackCard = findViewById(R.id.profile_feedback_card)
        aboutCard = findViewById(R.id.profile_about_card)
        rateCard = findViewById(R.id.profile_rate_card)
        clearPlansCard = findViewById(R.id.profile_clear_plans_card)  // Changed

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

        feedbackCard.setOnClickListener {
            sendFeedback()
        }

        aboutCard.setOnClickListener {
            showAboutDialog()
        }

        rateCard.setOnClickListener {
            Toast.makeText(this, "⭐ Rate PlanWise on Google Play", Toast.LENGTH_SHORT).show()
        }

        clearPlansCard.setOnClickListener {
            showClearPlansDialog()
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

    private fun sendFeedback() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("💬 Send Feedback")
        builder.setMessage("We'd love to hear your thoughts! What can we improve?")

        val input = EditText(this)
        input.hint = "Type your feedback here..."
        input.setPadding(32, 16, 32, 16)
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            val feedback = input.text.toString().trim()
            if (feedback.isNotEmpty()) {
                sendEmail(feedback)
            } else {
                Toast.makeText(this, "Please write something!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun sendEmail(feedback: String) {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "PlanWise User Feedback")
            putExtra(Intent.EXTRA_TEXT, """
                $feedback

                ---
                App Version: $versionName
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android: ${Build.VERSION.RELEASE}
            """.trimIndent())
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No email app found! Please email us at your-email@example.com", Toast.LENGTH_LONG).show()
        }
    }

    private fun showClearPlansDialog() {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Clear All Plans")
            .setMessage("Are you sure you want to delete all your plans? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                clearAllPlans()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllPlans() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@ProfileActivity)
                        .planDao()
                        .deleteAllPlans()
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "✅ All plans cleared", Toast.LENGTH_SHORT).show()
                    loadProfileStats()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "❌ Failed to clear plans: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
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

    //override fun onBackPressed() {
        //super.onBackPressed()
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    //}

    override fun onBackPressed() {
        // Just finish the activity - no custom transition
        super.onBackPressed()
    }
}