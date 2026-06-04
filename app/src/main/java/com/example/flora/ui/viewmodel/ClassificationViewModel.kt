package com.example.flora.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.database.PlantCareDatabase
import com.example.flora.data.database.entities.Disease
import com.example.flora.data.database.entities.FlowerSpecies
import com.example.flora.data.database.entities.PlantHealthLog
import com.example.flora.ml.DiseaseClassifier
import com.example.flora.ml.FlowerClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IdentificationUiState {
    data object Idle : IdentificationUiState()
    data object Scanning : IdentificationUiState()
    data class Result(
        val recognitions: List<FlowerClassifier.Recognition>,
        val topSpecies: FlowerSpecies?
    ) : IdentificationUiState()
    data class Error(val message: String) : IdentificationUiState()
}

data class DiagnosedDisease(
    val disease: Disease?,          // null if only from heuristic (unknown DB row)
    val displayName: String,
    val probability: Float,          // 0-1
    val reason: String,              // explanation string
    val severity: String,
    val source: String               // "camera" | "text" | "camera+text"
)

sealed class DiagnosisUiState {
    data object Idle : DiagnosisUiState()
    data object Analyzing : DiagnosisUiState()
    data class Result(
        val diseases: List<DiseaseClassifier.DiseaseRecognition>,     // legacy raw
        val matches: List<DiagnosedDisease>,
        val query: String
    ) : DiagnosisUiState()
    data class Error(val message: String) : DiagnosisUiState()
}

class ClassificationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantCareDatabase.getInstance(application)
    private val flowerClassifier = FlowerClassifier(application)
    private val diseaseClassifier = DiseaseClassifier(application)
    private val speciesDao = db.flowerSpeciesDao()
    private val diseaseDao = db.diseaseDao()
    private val healthLogDao = db.plantHealthLogDao()

    private val _identificationState = MutableStateFlow<IdentificationUiState>(IdentificationUiState.Idle)
    val identificationState: StateFlow<IdentificationUiState> = _identificationState.asStateFlow()

    private val _diagnosisState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.Idle)
    val diagnosisState: StateFlow<DiagnosisUiState> = _diagnosisState.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    // Typeahead suggestions for the disease search field. Updated as the user types.
    private val _diseaseSuggestions = MutableStateFlow<List<Disease>>(emptyList())
    val diseaseSuggestions: StateFlow<List<Disease>> = _diseaseSuggestions.asStateFlow()

    /** Full disease catalog from the local DB — drives the menu-style picker grid. */
    private val _allDiseases = MutableStateFlow<List<Disease>>(emptyList())
    val allDiseases: StateFlow<List<Disease>> = _allDiseases.asStateFlow()

    init {
        viewModelScope.launch { _allDiseases.value = diseaseDao.getAllOnce() }
    }

    fun identifyPlant(bitmap: Bitmap) {
        _capturedImage.value = bitmap
        _identificationState.value = IdentificationUiState.Scanning
        viewModelScope.launch {
            when (val res = flowerClassifier.classifyImage(bitmap)) {
                is FlowerClassifier.Result.Success -> {
                    val top = res.recognitions.first()
                    val species = speciesDao.getSpeciesByName(top.label)
                        ?: speciesDao.searchSpeciesFuzzy(top.label)
                    _identificationState.value = IdentificationUiState.Result(res.recognitions, species)
                }
                is FlowerClassifier.Result.Failure -> {
                    _identificationState.value = IdentificationUiState.Error(res.reason)
                }
            }
        }
    }

    fun diagnosePlant(bitmap: Bitmap?, query: String = "") {
        if (bitmap != null) _capturedImage.value = bitmap
        _diagnosisState.value = DiagnosisUiState.Analyzing
        viewModelScope.launch {
            val camera = if (bitmap != null) diseaseClassifier.classifyDisease(bitmap) else emptyList()
            val dbRows = diseaseDao.getAllOnce()
            val textMatches = if (query.isNotBlank()) matchByText(query, dbRows) else emptyList()
            val merged = mergeDiagnoses(camera, textMatches, dbRows, query)
            if (merged.isEmpty()) {
                _diagnosisState.value = DiagnosisUiState.Error(
                    if (query.isBlank() && bitmap == null) "Describe symptoms or capture a photo."
                    else "No matching condition. Try different keywords or a clearer photo."
                )
            } else {
                _diagnosisState.value = DiagnosisUiState.Result(camera, merged, query)
            }
        }
    }

    private fun matchByText(query: String, rows: List<Disease>): List<Pair<Disease, Pair<Float, String>>> {
        val tokens = query.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .distinct()
        if (tokens.isEmpty()) return emptyList()
        val out = mutableListOf<Pair<Disease, Pair<Float, String>>>()
        for (row in rows) {
            val hay = (row.name + " " + row.category + " " + row.symptoms + " " + row.affectedParts).lowercase()
            val hits = tokens.filter { it in hay }
            if (hits.isNotEmpty()) {
                val score = (hits.size.toFloat() / tokens.size).coerceAtMost(1f)
                out.add(row to (score to "Matched: ${hits.joinToString(", ")}"))
            }
        }
        return out.sortedByDescending { it.second.first }
    }

    private fun mergeDiagnoses(
        camera: List<DiseaseClassifier.DiseaseRecognition>,
        text: List<Pair<Disease, Pair<Float, String>>>,
        dbRows: List<Disease>,
        query: String
    ): List<DiagnosedDisease> {
        val result = linkedMapOf<String, DiagnosedDisease>()

        fun diseaseRowFor(name: String): Disease? =
            dbRows.firstOrNull { it.name.equals(name, ignoreCase = true) }

        // Seed with camera results (enriched with DB row if available)
        for (cam in camera) {
            val row = diseaseRowFor(cam.diseaseName)
            val reason = if (row != null) {
                "Visual analysis: ${row.symptoms.take(120)}"
            } else {
                "Visual analysis detected signs of ${cam.diseaseName.lowercase()}"
            }
            result[cam.diseaseName.lowercase()] = DiagnosedDisease(
                disease = row,
                displayName = cam.diseaseName,
                probability = cam.confidence,
                reason = reason,
                severity = cam.severity,
                source = "camera"
            )
        }

        // Merge text matches — boost existing, add new
        for ((row, scoreReason) in text) {
            val key = row.name.lowercase()
            val existing = result[key]
            val (textScore, textReason) = scoreReason
            if (existing != null) {
                val boosted = ((existing.probability + textScore) / 2f + 0.10f).coerceAtMost(0.98f)
                result[key] = existing.copy(
                    probability = boosted,
                    reason = "${existing.reason}\n$textReason",
                    source = "camera+text"
                )
            } else {
                result[key] = DiagnosedDisease(
                    disease = row,
                    displayName = row.name,
                    probability = textScore,
                    reason = textReason,
                    severity = severityFromLevel(row.severityLevel),
                    source = "text"
                )
            }
        }

        return result.values.sortedByDescending { it.probability }.take(5)
    }

    private fun severityFromLevel(level: Int): String = when (level) {
        1 -> "Mild"; 2 -> "Moderate"; 3 -> "Severe"; else -> "Unknown"
    }

    fun logDiagnosisForPlant(plantId: Int, diagnosed: DiagnosedDisease) {
        viewModelScope.launch {
            healthLogDao.insert(
                PlantHealthLog(
                    plantId = plantId,
                    logType = "Disease",
                    healthScore = (100 - (diagnosed.probability * 60).toInt()).coerceIn(30, 100),
                    notes = diagnosed.reason,
                    isResolved = false,
                    diseaseName = diagnosed.displayName
                )
            )
        }
    }

    /**
     * Live disease typeahead. Returns name + symptom matches in DB. Empty input → empty list.
     */
    fun updateDiseaseSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _diseaseSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _diseaseSuggestions.value = diseaseDao.searchByText("%$trimmed%", limit = 20)
        }
    }

    /** Run diagnosis against a specific disease the user picked from suggestions. */
    fun diagnoseByDisease(disease: Disease) {
        _diagnosisState.value = DiagnosisUiState.Analyzing
        viewModelScope.launch {
            val diagnosed = DiagnosedDisease(
                disease = disease,
                displayName = disease.name,
                probability = 0.92f,
                reason = "Selected from disease list. Symptoms: ${disease.symptoms}",
                severity = severityFromLevel(disease.severityLevel),
                source = "manual",
            )
            _diagnosisState.value = DiagnosisUiState.Result(
                diseases = emptyList(),
                matches = listOf(diagnosed),
                query = disease.name,
            )
        }
    }

    fun resetIdentification() {
        _identificationState.value = IdentificationUiState.Idle
        _capturedImage.value = null
    }

    fun resetDiagnosis() {
        _diagnosisState.value = DiagnosisUiState.Idle
        _capturedImage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        flowerClassifier.close()
        diseaseClassifier.close()
    }
}
