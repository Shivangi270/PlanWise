package com.planwise.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.planwise.app.data.Plan
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.launch

class PlanListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: PlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_list)

        recyclerView = findViewById(R.id.plan_recycler_view)
        emptyText = findViewById(R.id.empty_text)

        adapter = PlanAdapter(emptyList()) { plan ->
            android.widget.Toast.makeText(this, "Viewing: ${plan.goal}", android.widget.Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadPlans()
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        lifecycleScope.launch {
            PlanDatabase.getDatabase(this@PlanListActivity)
                .planDao()
                .getAllPlans()
                .collect { planList ->
                    if (planList.isEmpty()) {
                        emptyText.visibility = android.view.View.VISIBLE
                        recyclerView.visibility = android.view.View.GONE
                    } else {
                        emptyText.visibility = android.view.View.GONE
                        recyclerView.visibility = android.view.View.VISIBLE
                        adapter.updatePlans(planList)
                    }
                }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}