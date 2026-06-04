package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val name: String = "",
    val species: String = "",
    val malayName: String = "",
    val speciesId: Int? = null,
    val imageUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val location: String = "Garden",
    val notes: String = "",
    val healthStatus: String = "Healthy",
    val lastHealthCheck: Long? = null,
    val healthScore: Int = 100,
    val lastWatered: Long? = null,
    val wateringFrequencyDays: Int = 2,
    val sunlight: String = "Full sun",
    val fertilizer: String = "Monthly"
)
