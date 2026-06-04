package com.example.flora.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flora.data.database.entities.CareRequirements

@Dao
interface CareRequirementsDao {
    @Query("SELECT * FROM care_requirements WHERE speciesId = :speciesId LIMIT 1")
    suspend fun getBySpeciesId(speciesId: Int): CareRequirements?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requirements: List<CareRequirements>)
}
