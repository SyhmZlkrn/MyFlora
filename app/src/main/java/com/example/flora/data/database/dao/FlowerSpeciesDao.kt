package com.example.flora.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flora.data.database.entities.FlowerSpecies
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowerSpeciesDao {
    @Query("SELECT * FROM flower_species")
    fun getAllSpecies(): Flow<List<FlowerSpecies>>

    @Query("SELECT * FROM flower_species")
    suspend fun getAllOnce(): List<FlowerSpecies>

    @Query("SELECT * FROM flower_species WHERE commonName = :name LIMIT 1")
    suspend fun getSpeciesByName(name: String): FlowerSpecies?

    @Query("SELECT * FROM flower_species WHERE LOWER(commonName) LIKE '%' || LOWER(:name) || '%' OR LOWER(malayName) LIKE '%' || LOWER(:name) || '%' OR LOWER(scientificName) LIKE '%' || LOWER(:name) || '%' LIMIT 1")
    suspend fun searchSpeciesFuzzy(name: String): FlowerSpecies?

    @Query("SELECT * FROM flower_species WHERE id = :id")
    suspend fun getSpeciesById(id: Int): FlowerSpecies?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(species: List<FlowerSpecies>)
}
