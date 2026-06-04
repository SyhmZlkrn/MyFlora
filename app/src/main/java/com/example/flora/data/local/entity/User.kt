package com.example.flora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,           // stored as lowercase
    val passwordHash: String,
    val salt: String,
    val joinDate: String = "",   // e.g. "February 2026"
    val createdAt: Long = System.currentTimeMillis(),
    val profileImageUri: String? = null,
    /**
     * True once the user clicks the verification link sent to their email after
     * registration. Existing pre-migration users are treated as already verified.
     */
    val isVerified: Boolean = true,
)
