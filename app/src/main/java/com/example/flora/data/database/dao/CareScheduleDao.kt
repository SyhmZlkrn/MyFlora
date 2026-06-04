package com.example.flora.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flora.data.database.entities.CareSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface CareScheduleDao {
    @Query("SELECT * FROM care_schedules WHERE userId = :userId ORDER BY nextDueDate ASC")
    fun getTasksByUser(userId: Int): Flow<List<CareSchedule>>

    @Query("SELECT * FROM care_schedules WHERE userId = :userId AND plantId = :plantId ORDER BY nextDueDate ASC")
    fun getTasksByPlant(userId: Int, plantId: Int): Flow<List<CareSchedule>>

    @Query("UPDATE care_schedules SET isCompleted = :completed WHERE id = :taskId AND userId = :userId")
    suspend fun setCompleted(taskId: Int, userId: Int, completed: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: CareSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<CareSchedule>)

    @Update
    suspend fun update(schedule: CareSchedule)

    @Query("DELETE FROM care_schedules WHERE id = :taskId AND userId = :userId")
    suspend fun deleteById(taskId: Int, userId: Int)

    @Query("DELETE FROM care_schedules WHERE plantId = :plantId AND userId = :userId")
    suspend fun deleteByPlant(plantId: Int, userId: Int)

    @Query("DELETE FROM care_schedules WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)

    @Query("SELECT COUNT(*) FROM care_schedules WHERE userId = :userId")
    suspend fun getTaskCount(userId: Int): Int

    @Query("SELECT * FROM care_schedules WHERE isEnabled = 1 AND isCompleted = 0 AND nextDueDate <= :cutoffMillis ORDER BY nextDueDate ASC")
    suspend fun getDueBefore(cutoffMillis: Long): List<CareSchedule>

    @Query("SELECT * FROM care_schedules WHERE userId = :userId AND isEnabled = 1 AND isCompleted = 0 AND nextDueDate <= :cutoffMillis ORDER BY nextDueDate ASC")
    suspend fun getDueBeforeForUser(cutoffMillis: Long, userId: Int): List<CareSchedule>
}
