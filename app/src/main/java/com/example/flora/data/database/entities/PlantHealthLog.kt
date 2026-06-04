package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_health_logs")
data class PlantHealthLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int = 0,
    val logType: String = "",
    val healthScore: Int = 100,
    val notes: String? = null,
    val isResolved: Boolean = true,
    val diseaseName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
