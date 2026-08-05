package com.planwise.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.planwise.app.data.Plan
import com.planwise.app.data.PlanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: PlanAdapter
    private var currentPlans: List<Plan> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_list)

        recyclerView = findViewById(R.id.plan_recycler_view)
        emptyText = findViewById(R.id.empty_text)

        adapter = PlanAdapter(
            plans = emptyList(),
            onItemClick = { plan ->
                val intent = Intent(this, PlanDetailActivity::class.java)
                intent.putExtra("plan_id", plan.id)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            },
            onDeleteClick = { plan, position ->
                showDeleteConfirmationDialog(plan, position)
            }
        )
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
                    currentPlans = planList
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

    private fun showDeleteConfirmationDialog(plan: Plan, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Plan")
            .setMessage("The plan for '${plan.goal}' will be permanently deleted.")
            .setPositiveButton("Delete") { _, _ ->
                deletePlan(plan, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePlan(plan: Plan, position: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    PlanDatabase.getDatabase(this@PlanListActivity)
                        .planDao()
                        .deletePlan(plan)
                }
                
                val updatedList = currentPlans.toMutableList()
                updatedList.removeAt(position)
                currentPlans = updatedList
                adapter.updatePlans(updatedList)
                
                Toast.makeText(this@PlanListActivity, "✅ Plan deleted", Toast.LENGTH_SHORT).show()
                
                if (updatedList.isEmpty()) {
                    emptyText.visibility = android.view.View.VISIBLE
                    recyclerView.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlanListActivity, "❌ Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }
}