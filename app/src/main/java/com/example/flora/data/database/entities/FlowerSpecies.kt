package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flower_species")
data class FlowerSpecies(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commonName: String = "",
    val malayName: String = "",
    val scientificName: String = "",
    val careLevel: String = "Easy",
    val isNative: Boolean = false,
    val description: String = "",
    val bloomSeason: String = "Year-round",
    val wateringFrequency: String = "Every 2-3 days",
    val sunlight: String = "Full sun"
)
