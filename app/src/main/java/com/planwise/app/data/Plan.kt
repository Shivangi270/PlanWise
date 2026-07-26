package com.planwise.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goal: String,
    val deadline: Int,
    val dailyHours: Int,
    val role: String,
    val topics: String,
    val planText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false // New field
)