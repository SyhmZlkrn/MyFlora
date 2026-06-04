package com.example.flora.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flora.data.database.entities.Disease
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseDao {
    @Query("SELECT * FROM diseases")
    fun getAllDiseases(): Flow<List<Disease>>

    @Query("SELECT * FROM diseases")
    suspend fun getAllOnce(): List<Disease>

    @Query("SELECT * FROM diseases WHERE id = :id")
    suspend fun getDiseaseById(id: Int): Disease?

    @Query("SELECT * FROM diseases WHERE name = :name LIMIT 1")
    suspend fun getDiseaseByName(name: String): Disease?

    /**
     * Typeahead search across name + category + symptoms + affected parts.
     * Caller wraps the input as "%term%" — wildcards must be in the bound value.
     */
    @Query("""
        SELECT * FROM diseases
        WHERE name LIKE :q OR category LIKE :q OR symptoms LIKE :q OR affectedParts LIKE :q
        ORDER BY name
        LIMIT :limit
    """)
    suspend fun searchByText(q: String, limit: Int = 20): List<Disease>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diseases: List<Disease>)
}
