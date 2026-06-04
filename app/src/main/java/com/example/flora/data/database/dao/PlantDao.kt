package com.example.flora.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flora.data.database.entities.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants WHERE userId = :userId ORDER BY dateAdded DESC")
    fun getPlantsByUser(userId: Int): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getPlantByIdForUser(userId: Int, id: Int): Plant?

    @Query("SELECT * FROM plants WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR species LIKE '%' || :query || '%' OR malayName LIKE '%' || :query || '%')")
    fun searchPlants(userId: Int, query: String): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: Plant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<Plant>)

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)

    @Query("SELECT COUNT(*) FROM plants WHERE userId = :userId")
    suspend fun getPlantCount(userId: Int): Int

    @Query("SELECT * FROM plants WHERE userId = :userId")
    suspend fun getPlantsByUserOnce(userId: Int): List<Plant>

    @Query("DELETE FROM plants WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
