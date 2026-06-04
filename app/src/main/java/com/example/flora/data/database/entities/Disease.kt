package com.example.flora.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diseases")
data class Disease(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val symptoms: String = "",
    val treatment: String = "",
    val prevention: String = "",
    val severityLevel: Int = 1,
    val affectedParts: String = "Leaves"
)
