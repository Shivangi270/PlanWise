package com.planwise.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class PlanListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_list)

        // For now, show a placeholder
        val emptyText = findViewById<TextView>(R.id.empty_text)
        emptyText.text = "📋 Your plans will appear here\n\nCreate your first plan from the dashboard!"
    }
}