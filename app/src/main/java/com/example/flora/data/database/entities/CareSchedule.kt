package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "care_schedules")
data class CareSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val plantId: Int = 0,
    val plantName: String = "",
    val taskType: String = "",
    val intervalDays: Int = 1,
    val nextDueDate: Long = System.currentTimeMillis(),
    val dueTime: String = "08:00",
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val colorIndex: Int = 0
)
