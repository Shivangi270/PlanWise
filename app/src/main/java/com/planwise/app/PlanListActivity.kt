package com.planwise.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class PlanListActivity : AppCompatActivity() {

    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_list)

        emptyText = findViewById(R.id.empty_text)
        emptyText.text = "📋 Your plans will appear here\n\nCreate your first plan from the dashboard!"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}