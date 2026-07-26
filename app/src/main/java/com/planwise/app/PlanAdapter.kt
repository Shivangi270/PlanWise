package com.planwise.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.planwise.app.data.Plan

class PlanAdapter(
    private var plans: List<Plan>,
    private val onItemClick: (Plan) -> Unit,
    private val onDeleteClick: (Plan, Int) -> Unit  // Added delete callback
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val goalText: TextView = itemView.findViewById(R.id.plan_goal_text)
        val detailsText: TextView = itemView.findViewById(R.id.plan_details_text)
        val dateText: TextView = itemView.findViewById(R.id.plan_date_text)
        val deleteButton: View = itemView.findViewById(R.id.plan_delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        holder.goalText.text = plan.goal
        holder.detailsText.text = "${plan.dailyHours} hrs/day • ${plan.deadline} days • ${plan.role}"
        holder.dateText.text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(plan.createdAt))
        
        // Click to view plan details
        holder.itemView.setOnClickListener {
            onItemClick(plan)
        }
        
        // Delete button
        holder.deleteButton.setOnClickListener {
            onDeleteClick(plan, position)
        }
    }

    override fun getItemCount(): Int = plans.size

    fun updatePlans(newPlans: List<Plan>) {
        plans = newPlans
        notifyDataSetChanged()
    }
}