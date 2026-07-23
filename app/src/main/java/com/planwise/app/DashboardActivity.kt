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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        welcomeText = findViewById(R.id.welcome_text)
        createPlanButton = findViewById(R.id.create_plan_button)
        viewPlansCard = findViewById(R.id.view_plans_card)

        // Set welcome message
        welcomeText.text = "Welcome to PlanWise! 🎯\nPlan Smarter, Achieve More"

        // Create Plan button - goes to MainActivity (plan creation)
        createPlanButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // View Plans - for now, goes to MainActivity too
        viewPlansCard.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}