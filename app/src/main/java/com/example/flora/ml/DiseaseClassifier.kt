package com.example.flora.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.flora.data.api.PlantNetDiseaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Disease classifier — three-stage chain:
 *   1. On-device YOLOv8 disease detector ([PlantDiseaseTfliteClassifier])
 *   2. PlantNet /v2/diseases/identify cloud fallback ([PlantNetDiseaseApi])
 *   3. Colour-histogram heuristic so the user always gets *some* hint
 *
 * Returns existing [DiseaseRecognition] shape so the rest of the pipeline
 * (ClassificationViewModel → merge with text matches) is untouched.
 */
class DiseaseClassifier(
    private val context: Context,
    private val tflite: PlantDiseaseTfliteClassifier = PlantDiseaseTfliteClassifier(context),
    private val plantNet: PlantNetDiseaseApi = PlantNetDiseaseApi(),
) {

    data class DiseaseRecognition(
        val diseaseName: String,
        val confidence: Float,
        val severity: String,
    )

    /**
     * 1. Run TFLite YOLO disease detector → if hits → return.
     * 2. Else → colour heuristic (yellow/brown/white-spot/dark-spot ratios).
     */
    suspend fun classifyDisease(bitmap: Bitmap): List<DiseaseRecognition> = withContext(Dispatchers.IO) {
        // ── 1. On-device YOLO disease detection ──
        val tfliteResult = tflite.detect(bitmap)
        if (tfliteResult is PlantDiseaseTfliteClassifier.Result.Success) {
            val out = tfliteResult.predictions.map { p ->
                DiseaseRecognition(
                    diseaseName = p.label,
                    confidence = p.confidence,
                    severity = severityFor(p.label, p.confidence),
                )
            }
            Log.d(TAG, "TFLite hit: ${out.first().diseaseName} @ ${(out.first().confidence * 100).toInt()}%")
            return@withContext out
        }
        Log.d(TAG, "TFLite miss (${(tfliteResult as PlantDiseaseTfliteClassifier.Result.Failure).reason}); trying PlantNet cloud")

        // ── 2. PlantNet cloud disease ID ──
        when (val pn = plantNet.identify(bitmap)) {
            is PlantNetDiseaseApi.Result.Success -> {
                val out = pn.predictions.map { p ->
                    DiseaseRecognition(
                        diseaseName = p.displayName,
                        confidence = p.confidence,
                        severity = severityFor(p.displayName, p.confidence),
                    )
                }
                Log.d(TAG, "PlantNet hit: ${out.first().diseaseName} @ ${(out.first().confidence * 100).toInt()}%")
                return@withContext out
            }
            is PlantNetDiseaseApi.Result.Failure -> {
                Log.d(TAG, "PlantNet miss (${pn.reason}); falling back to colour heuristic")
            }
        }

        // ── 3. Fallback: legacy colour-histogram heuristic ──
        heuristic(bitmap)
    }

    fun close() {
        runCatching { tflite.close() }
    }

    private fun severityFor(label: String, confidence: Float): String {
        // Anything labelled "Healthy" → no severity issue.
        if (label.contains("Healthy", ignoreCase = true)) return "None"
        return when {
            confidence >= 0.70f -> "Severe"
            confidence >= 0.45f -> "Moderate"
            else -> "Mild"
        }
    }

    // ── Heuristic fallback ────────────────────────────────────────────

    private fun heuristic(bitmap: Bitmap): List<DiseaseRecognition> {
        val results = mutableListOf<DiseaseRecognition>()
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 100
        var yellowCount = 0
        var brownCount = 0
        var whiteSpotCount = 0
        var darkSpotCount = 0
        var greenCount = 0
        var totalSampled = 0

        for (x in 0 until width step (width / sampleSize).coerceAtLeast(1)) {
            for (y in 0 until height step (height / sampleSize).coerceAtLeast(1)) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                totalSampled++
                if (r > 150 && g > 130 && b < 100) yellowCount++
                if (r in 80..180 && g in 40..120 && b < 80) brownCount++
                if (r > 220 && g > 220 && b > 220) whiteSpotCount++
                if (r < 50 && g < 50 && b < 50) darkSpotCount++
                if (g > r && g > b && g > 100) greenCount++
            }
        }
        if (totalSampled == 0) return emptyList()

        val yellowRatio = yellowCount.toFloat() / totalSampled
        val brownRatio = brownCount.toFloat() / totalSampled
        val whiteRatio = whiteSpotCount.toFloat() / totalSampled
        val darkRatio = darkSpotCount.toFloat() / totalSampled
        val greenRatio = greenCount.toFloat() / totalSampled

        if (whiteRatio > 0.05f) {
            val conf = (whiteRatio * 3).coerceIn(0.4f, 0.95f)
            results.add(DiseaseRecognition("Powdery Mildew", conf, if (conf > 0.7f) "Moderate" else "Mild"))
        }
        if (darkRatio > 0.03f && greenRatio > 0.1f) {
            val conf = (darkRatio * 4).coerceIn(0.35f, 0.90f)
            results.add(DiseaseRecognition("Leaf Spot Disease", conf, if (conf > 0.7f) "Moderate" else "Mild"))
        }
        if (yellowRatio > 0.1f) {
            val conf = (yellowRatio * 2).coerceIn(0.4f, 0.85f)
            results.add(DiseaseRecognition("Nutrient Deficiency", conf, if (conf > 0.6f) "Moderate" else "Mild"))
        }
        if (brownRatio > 0.15f) {
            val conf = (brownRatio * 1.5f).coerceIn(0.4f, 0.88f)
            results.add(DiseaseRecognition("Root Rot", conf, "Severe"))
        }
        if (results.isEmpty() && greenRatio > 0.3f) {
            results.add(DiseaseRecognition("Healthy Plant", 0.90f, "None"))
        } else if (results.isEmpty()) {
            results.add(DiseaseRecognition("Unknown Condition", 0.5f, "Check manually"))
        }
        return results.sortedByDescending { it.confidence }
    }

    companion object {
        private const val TAG = "DiseaseClassifier"
    }
}
