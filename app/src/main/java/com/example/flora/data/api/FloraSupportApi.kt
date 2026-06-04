package com.example.flora.data.api

import android.util.Log
import com.example.flora.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Submits the in-app Help & Support form via **Web3Forms** so the user never
 * has to open an email app.
 *
 * Web3Forms is a no-backend form-to-email relay:
 *   1. Sign up at https://web3forms.com (free, 250 submits/month)
 *   2. Verify ownership of support@myfloraapp.com (it's where forms will be delivered)
 *   3. Paste the resulting access key into `local.properties`:
 *        WEB3FORMS_ACCESS_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
 *   4. Rebuild — done.
 *
 * Endpoint: POST https://api.web3forms.com/submit
 */
class FloraSupportApi(
    private val client: OkHttpClient = defaultClient(),
    private val accessKey: String = BuildConfig.WEB3FORMS_ACCESS_KEY,
) {

    sealed class Result {
        data object Success : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun sendMessage(
        name: String,
        email: String,
        category: String,
        message: String,
        appVersion: String = "1.0.0",
    ): Result = withContext(Dispatchers.IO) {
        if (accessKey.isBlank()) {
            return@withContext Result.Failure(
                "Support form not configured. Add WEB3FORMS_ACCESS_KEY to local.properties."
            )
        }

        val payload = JSONObject().apply {
            put("access_key", accessKey)
            put("from_name", name)
            put("email", email)
            put("subject", "[Flora] $category — from $name")
            put("category", category)
            put("message", message)
            put("app_version", appVersion)
            put("platform", "android")
        }.toString()

        val body = payload.toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder()
            .url(ENDPOINT)
            .header("Accept", "application/json")
            .header("User-Agent", "MyFlora-Android/$appVersion")
            .post(body)
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val ok = resp.isSuccessful && runCatching {
                    JSONObject(raw).optBoolean("success", false)
                }.getOrDefault(false)
                if (ok) {
                    Log.d(TAG, "Support message accepted")
                    Result.Success
                } else {
                    val msg = extractMessage(raw) ?: "HTTP ${resp.code}"
                    Log.w(TAG, "Support submit failed: $msg")
                    Result.Failure(msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Support submit error", e)
            Result.Failure("Network error: ${e.message ?: e::class.java.simpleName}")
        }
    }

    private fun extractMessage(raw: String): String? = try {
        JSONObject(raw).optString("message").ifBlank { null }
    } catch (_: Exception) { null }

    companion object {
        private const val TAG = "FloraSupportApi"
        private const val ENDPOINT = "https://api.web3forms.com/submit"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
