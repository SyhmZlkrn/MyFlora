package com.example.flora.data.api

import android.graphics.Bitmap
import android.util.Log
import com.example.flora.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * PlantNet v2 disease identification — cloud fallback for leaf-disease detection
 * when the on-device YOLOv8 disease model can't identify a condition.
 *
 * Docs:     https://my.plantnet.org/doc/api/diseases
 * Endpoint: POST https://my-api.plantnet.org/v2/diseases/identify
 *   multipart: image=<jpeg bytes>, organs=auto
 *   query:     api-key=<same PlantNet key as /v2/identify/all>
 *
 * Uses the same daily quota as plant species identification (1 credit per call).
 */
class PlantNetDiseaseApi(
    private val apiKey: String = BuildConfig.PLANTNET_API_KEY,
    private val client: OkHttpClient = defaultClient(),
) {

    data class Prediction(
        /** Human-readable disease name (e.g. "Powdery mildew"). */
        val displayName: String,
        /** EPPO disease code (e.g. "OIDIUM"). */
        val eppoCode: String,
        /** Confidence 0..1. */
        val confidence: Float,
    )

    sealed class Result {
        data class Success(val predictions: List<Prediction>) : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun identify(bitmap: Bitmap): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.Failure("PlantNet key missing. Set PLANTNET_API_KEY in local.properties.")
        }

        val jpeg = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "leaf.jpg",
                jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            .addFormDataPart("organs", "auto")
            .build()

        val url = "$BASE_URL?api-key=$apiKey&lang=en"
        val req = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = extractError(raw) ?: "HTTP ${resp.code}"
                    Log.w(TAG, "PlantNet disease fail: $msg")
                    return@withContext Result.Failure("PlantNet: $msg")
                }
                val preds = parse(raw)
                val top = preds.firstOrNull()
                when {
                    top == null -> Result.Failure("PlantNet returned no disease matches.")
                    top.confidence < MIN_CONFIDENCE -> Result.Failure(
                        "Not confident this leaf shows a known disease (top ${(top.confidence * 100).toInt()}%)."
                    )
                    else -> {
                        Log.d(TAG, "PlantNet top disease: ${top.displayName} @ ${(top.confidence * 100).toInt()}%")
                        Result.Success(preds)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PlantNet disease error", e)
            Result.Failure("PlantNet error: ${e.message ?: e::class.java.simpleName}")
        }
    }

    private fun parse(raw: String): List<Prediction> {
        val json = JSONObject(raw)
        val arr = json.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Prediction>(arr.length())
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val score = r.optDouble("score", 0.0).toFloat()
            val eppo = r.optString("name").trim()
            val desc = r.optString("description").trim().ifBlank { eppo }
            if (desc.isNotEmpty()) {
                out.add(Prediction(displayName = desc, eppoCode = eppo, confidence = score))
            }
        }
        return out
    }

    private fun extractError(raw: String): String? = try {
        JSONObject(raw).optString("message").ifBlank { null }
    } catch (_: Exception) { null }

    companion object {
        private const val TAG = "PlantNetDiseaseApi"
        private const val BASE_URL = "https://my-api.plantnet.org/v2/diseases/identify"
        // Below this PlantNet is guessing — likely not a recognizable disease.
        private const val MIN_CONFIDENCE = 0.10f

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
