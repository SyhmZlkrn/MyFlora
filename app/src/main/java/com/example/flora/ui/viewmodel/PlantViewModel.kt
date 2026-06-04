package com.example.flora.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.database.PlantCareDatabase
import com.example.flora.data.database.entities.CareSchedule
import com.example.flora.data.database.entities.Plant
import com.example.flora.data.database.entities.PlantHealthLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlantViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantCareDatabase.getInstance(application)
    private val plantDao = db.plantDao()
    private val careScheduleDao = db.careScheduleDao()
    private val healthLogDao = db.plantHealthLogDao()
    private val diseaseDao = db.diseaseDao()

    suspend fun getDiseaseByName(name: String) = diseaseDao.getDiseaseByName(name)

    private val _userId = MutableStateFlow<Int?>(null)

    val plants: StateFlow<List<Plant>> = _userId.flatMapLatest { uid ->
        uid?.let(plantDao::getPlantsByUser) ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Per-user active disease logs (unresolved). plantId → log (newest wins). */
    val activeDiseases: StateFlow<Map<Int, PlantHealthLog>> = _userId.flatMapLatest { uid ->
        uid?.let { healthLogDao.getActiveDiseaseLogsForUser(it) } ?: flowOf(emptyList())
    }
        .map { list -> list.associateBy { it.plantId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteHealthLog(logId: Int) {
        viewModelScope.launch { healthLogDao.deleteById(logId) }
    }

    fun markDiseaseResolved(log: PlantHealthLog) {
        viewModelScope.launch { healthLogDao.insert(log.copy(isResolved = true)) }
    }

    fun setUserId(id: Int?) {
        _userId.value = id
    }

    fun getLogsByPlant(plantId: Int): Flow<List<PlantHealthLog>> = _userId.flatMapLatest { uid ->
        uid?.let { healthLogDao.getLogsByPlantForUser(plantId, it) } ?: flowOf(emptyList())
    }

    suspend fun getPlantById(id: Int): Plant? {
        val userId = _userId.value ?: return null
        return plantDao.getPlantByIdForUser(userId, id)
    }

    fun addPlant(plant: Plant) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            plantDao.insert(plant.copy(userId = userId))
        }
    }

    fun addPlantWithStarterSchedule(plant: Plant) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            val savedPlantId = plantDao.insert(plant.copy(userId = userId)).toInt()
            val savedPlant = plantDao.getPlantByIdForUser(userId, savedPlantId) ?: return@launch
            val nextDueDate = (savedPlant.lastWatered ?: System.currentTimeMillis()) +
                (savedPlant.wateringFrequencyDays * 86_400_000L)

            careScheduleDao.insert(
                CareSchedule(
                    userId = userId,
                    plantId = savedPlantId,
                    plantName = savedPlant.name,
                    taskType = "Water",
                    intervalDays = savedPlant.wateringFrequencyDays,
                    nextDueDate = nextDueDate,
                    dueTime = "08:00",
                    colorIndex = savedPlantId
                )
            )
        }
    }

    fun updatePlant(plant: Plant) {
        viewModelScope.launch { plantDao.update(plant) }
    }

    fun deletePlant(plant: Plant) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            com.example.flora.util.BitmapStorage.delete(plant.imageUri)
            healthLogDao.deleteByPlant(plant.id)
            careScheduleDao.deleteByPlant(plant.id, userId)
            plantDao.delete(plant)
        }
    }

    fun updatePlantImage(plantId: Int, newPath: String) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            val plant = plantDao.getPlantByIdForUser(userId, plantId) ?: return@launch
            com.example.flora.util.BitmapStorage.delete(plant.imageUri)
            plantDao.update(plant.copy(imageUri = newPath))
        }
    }

    fun clearAllPlants() {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            val list = plantDao.getPlantsByUserOnce(userId)
            list.forEach { com.example.flora.util.BitmapStorage.delete(it.imageUri) }
            healthLogDao.deleteAllForUser(userId)
            careScheduleDao.deleteAllForUser(userId)
            plantDao.deleteAllForUser(userId)
        }
    }

    /** Delete a specific set of plants (selection-mode bulk delete). */
    fun deletePlants(ids: Set<Int>) {
        if (ids.isEmpty()) return
        val userId = _userId.value ?: return
        viewModelScope.launch {
            val all = plantDao.getPlantsByUserOnce(userId)
            all.filter { it.id in ids }.forEach { plant ->
                com.example.flora.util.BitmapStorage.delete(plant.imageUri)
                healthLogDao.deleteByPlant(plant.id)
                careScheduleDao.deleteByPlant(plant.id, userId)
                plantDao.delete(plant)
            }
        }
    }

    fun addHealthLog(log: PlantHealthLog) {
        viewModelScope.launch { healthLogDao.insert(log) }
    }

    /**
     * No longer seeds sample data — plants come from recognition + save only.
     */
    fun seedSamplePlantsIfNeeded() {
        // Intentionally empty — users build their collection via plant identification
    }
}
