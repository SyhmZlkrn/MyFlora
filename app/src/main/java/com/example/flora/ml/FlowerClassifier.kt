package com.example.flora.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.flora.data.api.PlantNetApiService

/**
 * Plant classifier — PlantNet REST API is the primary source; on-device TFLite
 * is the offline backup when the API is unreachable, key-less, or low-confidence.
 *
 * On-device backends (toggle [ACTIVE_BACKEND] below):
 *   - [Backend.PlantNet300k]  ResNet18 trained on 1081-class PlantNet-300K (DEFAULT)
 *   - [Backend.Roboflow]      YOLOv8 trained on 4-class Malaysian Flower Detection (dormant)
 *
 * Flow:
 *   1. Call PlantNet API. Confident hit                 → return cloud result.
 *   2. API failure (no key, offline, low conf, error)   → run on-device.
 *   3. On-device confident enough (≥[HARD_FAIL_THRESHOLD]) → return on-device.
 *   4. Both abstain                                     → "not a known plant".
 */
class FlowerClassifier(
    context: Context,
    private val backend: Backend = ACTIVE_BACKEND,
    // Both classifiers defer model load to first inference (internal `by lazy`),
    // so constructing both is cheap; the inactive one never opens its asset.
    private val plantNet300k: Plantnet300kClassifier = Plantnet300kClassifier(context),
    private val roboflow: RoboflowTfliteClassifier = RoboflowTfliteClassifier(context),
    private val plantNetApi: PlantNetApiService = PlantNetApiService(),
) {

    enum class Backend { PlantNet300k, Roboflow }

    enum class Source { OnDevice, PlantNet }

    data class Recognition(
        val label: String,
        val confidence: Float,
        val scientificName: String = "",
        val source: Source = Source.OnDevice,
    )

    sealed class Result {
        data class Success(val recognitions: List<Recognition>) : Result()
        data class Failure(val reason: String) : Result()
    }

    suspend fun classifyImage(bitmap: Bitmap): Result {
        // Both classifiers were trained ONLY on plants — they have no "not a plant"
        // class, so any input (floor, hand, etc.) gets mapped to the nearest plant.
        // We compensate with strict confidence + margin gates before surfacing a
        // result, treating uncertain peaks as out-of-distribution rather than as
        // valid plant matches.

        // 1) PlantNet API first.
        if (USE_CLOUD_PRIMARY) {
            when (val pn = plantNetApi.identify(bitmap)) {
                is PlantNetApiService.Result.Success -> {
                    val top = pn.predictions.first().confidence
                    val second = pn.predictions.getOrNull(1)?.confidence ?: 0f
                    val margin = top - second
                    if (top >= CLOUD_TRUST_THRESHOLD && margin >= CLOUD_MIN_MARGIN) {
                        Log.d(
                            TAG,
                            "PlantNet PRIMARY ok top=${(top * 100).toInt()}% margin=${"%.2f".format(margin)} → ${pn.predictions.first().scientificName}"
                        )
                        return Result.Success(
                            pn.predictions.map {
                                Recognition(
                                    label = it.commonName,
                                    confidence = it.confidence,
                                    scientificName = it.scientificName,
                                    source = Source.PlantNet,
                                )
                            }
                        )
                    } else {
                        Log.d(
                            TAG,
                            "PlantNet PRIMARY uncertain top=${(top * 100).toInt()}% margin=${"%.2f".format(margin)} (need >=${(CLOUD_TRUST_THRESHOLD * 100).toInt()}% / ${CLOUD_MIN_MARGIN}) → trying on-device backup"
                        )
                    }
                }
                is PlantNetApiService.Result.Failure -> {
                    Log.d(TAG, "PlantNet PRIMARY failed: ${pn.reason} → trying on-device backup")
                }
            }
        }

        // 2) On-device backup. 1081-class softmax has very low absolute peaks even
        // on real plants; require confident peak AND clear margin AND concentrated
        // distribution before trusting it.
        val onDevice = runOnDevice(bitmap)
        if (onDevice !is OnDeviceOutcome.Ok) {
            return Result.Failure(
                "Couldn't identify a plant in this photo. Try a closer, well-lit shot of a single flower or leaf."
            )
        }
        val confident = onDevice.topConfidence >= ON_DEVICE_TRUST_THRESHOLD
        val peaked = onDevice.margin >= ON_DEVICE_MIN_MARGIN
        val lowEntropy = onDevice.entropy <= ON_DEVICE_MAX_ENTROPY
        return if (confident && peaked && lowEntropy) {
            Log.d(TAG, "On-device BACKUP ok ${onDevice.summary} → ${onDevice.recognitions.first().label}")
            Result.Success(onDevice.recognitions)
        } else {
            Log.d(
                TAG,
                "On-device BACKUP rejected ${onDevice.summary} (need top>=${ON_DEVICE_TRUST_THRESHOLD} margin>=${ON_DEVICE_MIN_MARGIN} H<=${ON_DEVICE_MAX_ENTROPY})"
            )
            Result.Failure(
                "This doesn't look like a flower or leaf. Try a closer, well-lit photo with the plant filling most of the frame."
            )
        }
    }

    fun close() {
        runCatching { plantNet300k.close() }
        runCatching { roboflow.close() }
    }

    // ── Backend dispatch ──────────────────────────────────────────────

    private sealed class OnDeviceOutcome {
        data class Ok(
            val recognitions: List<Recognition>,
            val topConfidence: Float,
            val margin: Float,
            val entropy: Float,
        ) : OnDeviceOutcome() {
        }
        data class Fail(val reason: String) : OnDeviceOutcome()

        val summary: String
            get() = when (this) {
                is Ok -> "top=${(topConfidence * 100).toInt()}% margin=${"%.2f".format(margin)} H=${"%.2f".format(entropy)}"
                is Fail -> reason
            }
    }

    private suspend fun runOnDevice(bitmap: Bitmap): OnDeviceOutcome = when (backend) {
        Backend.PlantNet300k -> when (val r = plantNet300k.classify(bitmap)) {
            is Plantnet300kClassifier.Result.Success -> {
                val recs = r.predictions.map {
                    Recognition(
                        // Prefer common name for display; fallback to scientific.
                        label = it.commonName.ifBlank { it.label },
                        confidence = it.confidence,
                        scientificName = it.label,
                        source = Source.OnDevice,
                    )
                }
                OnDeviceOutcome.Ok(
                    recognitions = recs,
                    topConfidence = r.predictions.first().confidence,
                    margin = r.margin,
                    entropy = r.entropy,
                )
            }
            is Plantnet300kClassifier.Result.Failure -> OnDeviceOutcome.Fail(r.reason)
        }

        Backend.Roboflow -> when (val r = roboflow.detect(bitmap)) {
            is RoboflowTfliteClassifier.Result.Success -> {
                val recs = r.predictions.map {
                    Recognition(
                        label = it.label,
                        confidence = it.confidence,
                        scientificName = "",
                        source = Source.OnDevice,
                    )
                }
                // Roboflow doesn't expose margin/entropy; treat as "strong" iff conf high.
                val top = r.predictions.first().confidence
                OnDeviceOutcome.Ok(recs, topConfidence = top, margin = 1f, entropy = 0f)
            }
            is RoboflowTfliteClassifier.Result.Failure -> OnDeviceOutcome.Fail(r.reason)
        }
    }

    companion object {
        private const val TAG = "FlowerClassifier"

        /**
         * Active on-device backend. Flip to [Backend.Roboflow] to revert to the
         * 4-class YOLOv8 path without removing it.
         */
        val ACTIVE_BACKEND: Backend = Backend.PlantNet300k

        /**
         * Cloud-primary toggle. When `true`, PlantNet REST is tried first and the
         * on-device classifier is only used as offline backup. Flip to `false` to
         * skip the network call entirely (e.g. for offline-only builds).
         */
        const val USE_CLOUD_PRIMARY: Boolean = true

        // ── Cloud (PlantNet API) abstain gates ───────────────────────────
        // PlantNet always returns *something*. Floors, hands, blurred scenes get
        // mapped to low-conf plant guesses (typically 0.05–0.30). Require a peaked
        // distribution before trusting.
        private const val CLOUD_TRUST_THRESHOLD = 0.40f
        private const val CLOUD_MIN_MARGIN = 0.10f

        // ── On-device AlexNet (1081-class) abstain gates ─────────────────
        // 1081-class softmax baseline ≈ 0.001. Real-plant peaks typically 0.15–0.7.
        // Floor/non-plant inputs spread mass — entropy stays high, margin tiny.
        // Require all three to agree before surfacing.
        private const val ON_DEVICE_TRUST_THRESHOLD = 0.30f
        private const val ON_DEVICE_MIN_MARGIN = 0.10f
        private const val ON_DEVICE_MAX_ENTROPY = 0.65f
    }
}
