package com.example.flora.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * On-device plant classifier backed by PlantNet-300K (1081 species).
 *
 * Repo: https://github.com/plantnet/PlantNet-300K
 * Architecture: ResNet18, ImageNet-style preprocessing.
 *
 * Expected assets:
 *   models/plantnet300k.tflite       — ResNet18 float32 TFLite (with embedded softmax)
 *   models/plantnet300k_labels.txt   — 1081 scientific species names, line index = class id
 *
 * Input:  [1, 224, 224, 3] float32, NHWC, ImageNet-normalized
 * Output: [1, 1081] float32 softmax probabilities
 *
 * Preprocess pipeline matches torchvision pretrained-model convention:
 *   resize-shortest-side 256 -> center-crop 224 -> /255 -> (x - mean) / std
 */
class Plantnet300kClassifier(context: Context) {

    data class Prediction(
        val label: String,
        val confidence: Float,
        val commonName: String = "",
    )

    sealed class Result {
        data class Success(
            val predictions: List<Prediction>,
            /** Confidence margin: top1 - top2. Low margin = uncertain pick. */
            val margin: Float,
            /** Normalized Shannon entropy in [0,1]. Near 1.0 = very uncertain. */
            val entropy: Float,
        ) : Result()
        data class Failure(val reason: String) : Result()
    }

    private var initError: String? = null
    private val labels: List<String> = loadLabels(context)
    private val commonNames: Map<String, String> = loadCommonNames(context)
    private var inputSize: Int = INPUT_SIZE
    private var numClasses: Int = labels.size

    private val interpreter: Interpreter? by lazy {
        runCatching {
            val options = Interpreter.Options().apply {
                setNumThreads(2)
                setUseXNNPACK(true)
            }
            val buffer = loadModel(context)
            Interpreter(buffer, options).also { interp ->
                val inputShape = interp.getInputTensor(0).shape()
                val outputShape = interp.getOutputTensor(0).shape()
                if (inputShape.size == 4) inputSize = inputShape[1]
                if (outputShape.size == 2) numClasses = outputShape[1]
                Log.d(TAG, "TFLite loaded  in=${inputShape.toList()}  out=${outputShape.toList()}  labels=${labels.size}")
                if (labels.isNotEmpty() && numClasses != labels.size) {
                    Log.w(TAG, "Label count ${labels.size} != model numClasses $numClasses")
                }
            }
        }.onFailure {
            initError = it.message ?: it::class.java.simpleName
            Log.e(TAG, "TFLite init failed", it)
        }.getOrNull()
    }

    suspend fun classify(bitmap: Bitmap): Result {
        val interp = interpreter ?: return Result.Failure(
            "PlantNet-300K model unavailable." + (initError?.let { "\n\nReason: $it" } ?: "")
        )
        if (labels.isEmpty()) return Result.Failure(
            "Labels missing. Drop plantnet300k_labels.txt into app/src/main/assets/models/."
        )

        return try {
            // Test-time augmentation: original + horizontal flip, average softmax probs.
            // ResNet/EfficientNet are not flip-invariant; averaging fixes off-axis bias and
            // typically nets +1-2% top-1 at the cost of double inference.
            val originalInput = preprocess(bitmap, flipHorizontal = false)
            val flippedInput = preprocess(bitmap, flipHorizontal = true)
            val outA = Array(1) { FloatArray(numClasses) }
            val outB = Array(1) { FloatArray(numClasses) }
            interp.run(originalInput, outA)
            interp.run(flippedInput, outB)
            val probs = FloatArray(numClasses) { idx -> (outA[0][idx] + outB[0][idx]) * 0.5f }

            val preds = topK(probs, TOP_RESULTS, MIN_CONFIDENCE)
            val (top1, top2) = topTwoScores(probs)
            val margin = top1 - top2
            val entropy = normalizedEntropy(probs)

            if (preds.isEmpty()) {
                Result.Failure(
                    "Not confident this is a known plant species. Try a closer, well-lit photo of the flower."
                )
            } else {
                Log.d(
                    TAG,
                    "Top: ${preds.first().label} @ ${(preds.first().confidence * 100).toInt()}%  " +
                        "margin=${"%.3f".format(margin)}  H=${"%.3f".format(entropy)}"
                )
                Result.Success(preds, margin = margin, entropy = entropy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            Result.Failure("Inference error: ${e.message ?: e::class.java.simpleName}")
        }
    }

    fun close() {
        runCatching { interpreter?.close() }
    }

    // ── TFLite plumbing ───────────────────────────────────────────────

    private fun loadModel(context: Context): ByteBuffer {
        val afd = context.assets.openFd(MODEL_ASSET)
        val fis = java.io.FileInputStream(afd.fileDescriptor)
        return fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    private fun loadLabels(context: Context): List<String> {
        return try {
            BufferedReader(InputStreamReader(context.assets.open(LABELS_ASSET))).useLines { seq ->
                seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }.also { Log.d(TAG, "Loaded ${it.size} labels") }
        } catch (e: Exception) {
            Log.w(TAG, "Labels missing: ${e.message}")
            emptyList()
        }
    }

    /** Load common names JSON if present (sci -> common). Missing file => empty map. */
    private fun loadCommonNames(context: Context): Map<String, String> {
        return try {
            val raw = context.assets.open(COMMON_NAMES_ASSET).bufferedReader().use { it.readText() }
            val map = HashMap<String, String>()
            val obj = org.json.JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k).trim()
                if (v.isNotEmpty()) map[k] = v
            }
            Log.d(TAG, "Loaded ${map.size} common names")
            map
        } catch (e: Exception) {
            Log.w(TAG, "Common names missing: ${e.message}")
            emptyMap()
        }
    }

    private fun commonNameFor(scientific: String): String {
        commonNames[scientific]?.let { return it }
        // Try without authorship suffix.
        val genusSpecies = scientific.split(" ").take(2).joinToString(" ")
        commonNames[genusSpecies]?.let { return it }
        return ""
    }

    // ── Preprocessing: torchvision-style resize + center-crop + ImageNet norm ──

    private fun preprocess(bitmap: Bitmap, flipHorizontal: Boolean): ByteBuffer {
        // Resize shortest side proportionally to inputSize/CROP_RATIO (~256 for 224, ~342 for 300).
        val resizeShortest = (inputSize / CROP_RATIO).toInt()
        val w = bitmap.width
        val h = bitmap.height
        val scale = resizeShortest.toFloat() / min(w, h)
        val newW = max(1, (w * scale).toInt())
        val newH = max(1, (h * scale).toInt())
        val resized = if (newW == w && newH == h) bitmap
                      else Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        // Center-crop to inputSize x inputSize.
        val cropX = max(0, (newW - inputSize) / 2)
        val cropY = max(0, (newH - inputSize) / 2)
        val cropW = min(inputSize, newW - cropX)
        val cropH = min(inputSize, newH - cropY)
        val cropped = if (cropW == inputSize && cropH == inputSize)
            Bitmap.createBitmap(resized, cropX, cropY, inputSize, inputSize)
        else
            // Fallback: scale tiny image directly to input size.
            Bitmap.createScaledBitmap(resized, inputSize, inputSize, true)

        val pixels = IntArray(inputSize * inputSize)
        cropped.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val buffer = ByteBuffer
            .allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())

        // ImageNet normalization in NHWC float32 RGB.
        // Flip via row-wise reverse-x indexing.
        for (y in 0 until inputSize) {
            val rowStart = y * inputSize
            if (flipHorizontal) {
                for (x in inputSize - 1 downTo 0) {
                    putPixel(buffer, pixels[rowStart + x])
                }
            } else {
                for (x in 0 until inputSize) {
                    putPixel(buffer, pixels[rowStart + x])
                }
            }
        }
        buffer.rewind()

        if (resized !== bitmap) resized.recycle()
        if (cropped !== resized && cropped !== bitmap) cropped.recycle()
        return buffer
    }

    private fun putPixel(buffer: ByteBuffer, pixel: Int) {
        val r = ((pixel shr 16) and 0xFF) / 255f
        val g = ((pixel shr 8) and 0xFF) / 255f
        val b = (pixel and 0xFF) / 255f
        buffer.putFloat((r - MEAN_R) / STD_R)
        buffer.putFloat((g - MEAN_G) / STD_G)
        buffer.putFloat((b - MEAN_B) / STD_B)
    }

    // ── Top-K with confidence threshold ───────────────────────────────

    private fun topK(probs: FloatArray, k: Int, threshold: Float): List<Prediction> {
        // Indexed sort, descending; take first k that pass threshold.
        val indices = probs.indices.sortedByDescending { probs[it] }
        val out = ArrayList<Prediction>(k)
        for (idx in indices) {
            val score = probs[idx]
            if (score < threshold) break
            val sci = labels.getOrNull(idx) ?: continue
            out.add(Prediction(label = sci, confidence = score, commonName = commonNameFor(sci)))
            if (out.size >= k) break
        }
        return out
    }

    private fun topTwoScores(probs: FloatArray): Pair<Float, Float> {
        var t1 = Float.NEGATIVE_INFINITY
        var t2 = Float.NEGATIVE_INFINITY
        for (p in probs) {
            if (p > t1) { t2 = t1; t1 = p }
            else if (p > t2) { t2 = p }
        }
        if (t2 == Float.NEGATIVE_INFINITY) t2 = 0f
        return t1 to t2
    }

    /** Shannon entropy normalized to [0,1] by log(numClasses). */
    private fun normalizedEntropy(probs: FloatArray): Float {
        var h = 0.0
        for (p in probs) {
            if (p > 1e-8f) h -= p * kotlin.math.ln(p.toDouble())
        }
        val maxH = kotlin.math.ln(probs.size.toDouble())
        return (h / maxH).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "PlantNet300k"
        private const val MODEL_ASSET = "models/plantnet300k.tflite"
        private const val LABELS_ASSET = "models/plantnet300k_labels.txt"
        private const val COMMON_NAMES_ASSET = "models/plantnet300k_common_names.json"
        // Default; overridden at runtime from the model's input tensor shape.
        // EfficientNet-B3 (current) → 300; ResNet18 → 224.
        private const val INPUT_SIZE = 300
        private const val CROP_RATIO = 0.875f  // standard ImageNet eval crop ratio (224/256)
        private const val TOP_RESULTS = 3

        // ImageNet stats (torchvision pretrained convention).
        private const val MEAN_R = 0.485f
        private const val MEAN_G = 0.456f
        private const val MEAN_B = 0.406f
        private const val STD_R = 0.229f
        private const val STD_G = 0.224f
        private const val STD_B = 0.225f

        // 1081-class softmax has long tail; raw top score for true plants typically 0.05–0.6.
        // Keep low to surface plausible matches; FlowerClassifier gates further.
        private const val MIN_CONFIDENCE = 0.03f
    }
}
