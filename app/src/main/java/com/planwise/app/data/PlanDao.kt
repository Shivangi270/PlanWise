package com.planwise.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert
    suspend fun insertPlan(plan: Plan)

    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<Plan>>

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("DELETE FROM plans")
    suspend fun deleteAllPlans()
}