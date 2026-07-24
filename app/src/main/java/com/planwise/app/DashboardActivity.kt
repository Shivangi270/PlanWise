package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var createPlanButton: CardView
    private lateinit var viewPlansCard: CardView
    private lateinit var emptyCreateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        welcomeText = findViewById(R.id.welcome_text)
        createPlanButton = findViewById(R.id.create_plan_button)
        viewPlansCard = findViewById(R.id.view_plans_card)
        emptyCreateButton = findViewById(R.id.empty_create_button)

        // Set welcome message with time-based greeting
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning! 👋"
            in 12..16 -> "Good Afternoon! 🌤️"
            in 17..20 -> "Good Evening! 🌅"
            else -> "Good Night! 🌙"
        }
        welcomeText.text = "$greeting\nPlan Smarter, Achieve More"

        // Create Plan button - goes to MainActivity (plan creation)
        createPlanButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // View Plans - goes to PlanListActivity
        viewPlansCard.setOnClickListener {
            val intent = Intent(this, PlanListActivity::class.java)
            startActivity(intent)
        }

        // Empty state create button
        emptyCreateButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}