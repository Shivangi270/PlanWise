package com.planwise.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert
    suspend fun insertPlan(plan: Plan)

    @Update
    suspend fun updatePlan(plan: Plan)

    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): Plan?

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("DELETE FROM plans")
    suspend fun deleteAllPlans()
}