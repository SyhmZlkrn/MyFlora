package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "care_requirements")
data class CareRequirements(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val speciesId: Int = 0,
    val wateringFrequencyDays: Int = 2,
    val sunlight: String = "Full sun",
    val fertilizerFrequency: String = "Monthly"
)
