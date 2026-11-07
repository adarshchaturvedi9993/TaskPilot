package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val rawInstruction: String,
    val status: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val failedSteps: Int,
    val durationMs: Long,
    val timestamp: Long,
    val stepsJson: String,
    val errorMessage: String? = null
)
