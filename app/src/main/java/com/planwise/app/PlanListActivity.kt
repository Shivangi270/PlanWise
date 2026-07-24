package com.planwise.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class PlanListActivity : AppCompatActivity() {

    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_list)

        // Initialize views
        emptyText = findViewById(R.id.empty_text)
        emptyText.text = "📋 Your plans will appear here\n\nCreate your first plan from the dashboard!"
    }
}