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
 * PlantNet v2 identify endpoint — cloud fallback when on-device TFLite
 * can't confidently ID the flower.
 *
 * Docs: https://my.plantnet.org/doc/openapi
 * Endpoint: POST https://my-api.plantnet.org/v2/identify/all
 *   multipart: images=<jpeg bytes>, organs=flower
 *   query:     api-key=<key>, nb-results=<n>
 */
class PlantNetApiService(
    private val apiKey: String = BuildConfig.PLANTNET_API_KEY,
    private val client: OkHttpClient = defaultClient(),
) {

    data class Prediction(
        val commonName: String,
        val scientificName: String,
        val confidence: Float,
    )

    sealed class Result {
        data class Success(val predictions: List<Prediction>) : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun identify(bitmap: Bitmap, maxResults: Int = 3): Result = withContext(Dispatchers.IO) {
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
                "images",
                "flower.jpg",
                jpeg.toRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            .addFormDataPart("organs", "flower")
            .build()

        val url = "$BASE_URL?api-key=$apiKey&nb-results=$maxResults&lang=en"
        val req = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = extractError(raw) ?: "HTTP ${resp.code}"
                    Log.w(TAG, "PlantNet fail: $msg")
                    return@withContext Result.Failure("PlantNet: $msg")
                }
                val preds = parse(raw)
                val top = preds.firstOrNull()
                when {
                    top == null -> Result.Failure("No plant match. Try a clearer photo of a flower.")
                    top.confidence < MIN_CONFIDENCE -> Result.Failure(
                        "Not confident this is a flower (top match ${(top.confidence * 100).toInt()}%). Try a closer shot."
                    )
                    else -> {
                        Log.d(TAG, "PlantNet top: ${top.scientificName} @ ${(top.confidence * 100).toInt()}%")
                        Result.Success(preds)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PlantNet error", e)
            Result.Failure("PlantNet error: ${e.message ?: e::class.java.simpleName}")
        }
    }

    private fun parse(raw: String): List<Prediction> {
        val json = JSONObject(raw)
        val arr = json.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Prediction>(arr.length())
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val species = r.optJSONObject("species") ?: continue
            val sci = species.optString("scientificNameWithoutAuthor").trim()
            val commons = species.optJSONArray("commonNames")
            val common = if (commons != null && commons.length() > 0) commons.optString(0) else sci
            val score = r.optDouble("score", 0.0).toFloat()
            if (sci.isNotEmpty()) {
                out.add(Prediction(commonName = common.ifBlank { sci }, scientificName = sci, confidence = score))
            }
        }
        return out
    }

    private fun extractError(raw: String): String? = try {
        JSONObject(raw).optString("message").ifBlank { null }
    } catch (_: Exception) { null }

    companion object {
        private const val TAG = "PlantNetApi"
        private const val BASE_URL = "https://my-api.plantnet.org/v2/identify/all"
        // Below this PlantNet is guessing — probably not a plant at all.
        private const val MIN_CONFIDENCE = 0.15f

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
