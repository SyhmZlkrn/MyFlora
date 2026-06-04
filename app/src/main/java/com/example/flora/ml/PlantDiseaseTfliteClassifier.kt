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

/**
 * On-device disease detector backed by a Roboflow-exported YOLOv8n TFLite model
 * trained on the `yolo-v8-solrb/plant-disease-ep6jy` dataset (20 classes).
 *
 * Expected assets:
 *   models/plant_disease.tflite        — YOLOv8n TFLite export (float32, 320x320)
 *   models/plant_disease_labels.txt    — English class names, one per line (order matches data.yaml)
 *
 * Output shape (YOLOv8): [1, 4 + numClasses, numAnchors]
 *   rows 0..3              → x, y, w, h in pixel coords
 *   rows 4..3+numClasses   → per-class sigmoid scores
 */
class PlantDiseaseTfliteClassifier(context: Context) {

    data class Prediction(
        val label: String,
        val confidence: Float,
    )

    sealed class Result {
        data class Success(val predictions: List<Prediction>) : Result()
        data class Failure(val reason: String) : Result()
    }

    private var initError: String? = null
    private val labels: List<String> = loadLabels(context)
    private var inputSize: Int = DEFAULT_INPUT_SIZE
    private var numClasses: Int = 0
    private var numAnchors: Int = 0

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
                if (outputShape.size == 3) {
                    numClasses = outputShape[1] - 4
                    numAnchors = outputShape[2]
                }
                Log.d(TAG, "TFLite loaded  in=${inputShape.toList()}  out=${outputShape.toList()}  nc=$numClasses  anchors=$numAnchors")
                if (labels.isNotEmpty() && numClasses > 0 && numClasses != labels.size) {
                    Log.w(TAG, "Label count ${labels.size} != model numClasses $numClasses")
                }
            }
        }.onFailure {
            initError = it.message ?: it::class.java.simpleName
            Log.e(TAG, "TFLite init failed", it)
        }.getOrNull()
    }

    suspend fun detect(bitmap: Bitmap): Result {
        val interp = interpreter ?: return Result.Failure(
            "Disease model unavailable." + (initError?.let { "\n\nReason: $it" } ?: "")
        )
        if (labels.isEmpty()) return Result.Failure(
            "Disease labels missing. Drop plant_disease_labels.txt into assets/models/."
        )

        return try {
            val input = preprocess(bitmap)
            val output = Array(1) { Array(4 + numClasses) { FloatArray(numAnchors) } }
            interp.run(input, output)

            val preds = postprocessYolo(output[0])
            if (preds.isEmpty()) {
                Result.Failure("No disease detected. Try a closer, well-lit photo of the affected leaf.")
            } else {
                Log.d(TAG, "Top: ${preds.first().label} @ ${(preds.first().confidence * 100).toInt()}%")
                Result.Success(preds)
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
            }.also { Log.d(TAG, "Loaded ${it.size} disease labels") }
        } catch (e: Exception) {
            Log.w(TAG, "Disease labels missing: ${e.message}")
            emptyList()
        }
    }

    // ── Preprocessing: YOLOv8 expects float32 RGB [0,1] in NHWC ────────

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val buffer = ByteBuffer
            .allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
        buffer.rewind()
        if (resized !== bitmap) resized.recycle()
        return buffer
    }

    // ── YOLOv8 postprocessing: filter by score + NMS ─────────────────

    private fun postprocessYolo(out: Array<FloatArray>): List<Prediction> {
        val nc = numClasses
        val anchors = numAnchors
        val candidates = ArrayList<Box>(128)

        for (i in 0 until anchors) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 0 until nc) {
                val s = out[4 + c][i]
                if (s > bestScore) { bestScore = s; bestClass = c }
            }
            if (bestScore < SCORE_THRESHOLD) continue
            val cx = out[0][i]; val cy = out[1][i]
            val w  = out[2][i]; val h  = out[3][i]
            val x1 = cx - w / 2f
            val y1 = cy - h / 2f
            val x2 = cx + w / 2f
            val y2 = cy + h / 2f
            candidates.add(Box(x1, y1, x2, y2, bestScore, bestClass))
        }
        if (candidates.isEmpty()) return emptyList()

        val kept = nms(candidates, IOU_THRESHOLD)
        val perClass = linkedMapOf<Int, Float>()
        for (b in kept) {
            val existing = perClass[b.cls]
            if (existing == null || b.score > existing) perClass[b.cls] = b.score
        }
        return perClass.entries
            .sortedByDescending { it.value }
            .take(TOP_RESULTS)
            .mapNotNull { (cls, score) ->
                val label = labels.getOrNull(cls) ?: return@mapNotNull null
                Prediction(label = label, confidence = score)
            }
    }

    private data class Box(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val score: Float, val cls: Int,
    ) {
        fun area(): Float = (x2 - x1).coerceAtLeast(0f) * (y2 - y1).coerceAtLeast(0f)
    }

    private fun iou(a: Box, b: Box): Float {
        val ix1 = maxOf(a.x1, b.x1); val iy1 = maxOf(a.y1, b.y1)
        val ix2 = minOf(a.x2, b.x2); val iy2 = minOf(a.y2, b.y2)
        val iw = (ix2 - ix1).coerceAtLeast(0f); val ih = (iy2 - iy1).coerceAtLeast(0f)
        val inter = iw * ih
        val union = a.area() + b.area() - inter
        return if (union <= 0f) 0f else inter / union
    }

    private fun nms(boxes: List<Box>, iouThresh: Float): List<Box> {
        val sorted = boxes.sortedByDescending { it.score }.toMutableList()
        val kept = ArrayList<Box>(sorted.size)
        while (sorted.isNotEmpty()) {
            val top = sorted.removeAt(0)
            kept.add(top)
            sorted.removeAll { b -> b.cls == top.cls && iou(top, b) > iouThresh }
        }
        return kept
    }

    companion object {
        private const val TAG = "PlantDiseaseTflite"
        private const val MODEL_ASSET = "models/plant_disease.tflite"
        private const val LABELS_ASSET = "models/plant_disease_labels.txt"
        private const val DEFAULT_INPUT_SIZE = 320
        private const val TOP_RESULTS = 3
        // Model trained to mAP50 0.50 — lower threshold than the flower path so it actually fires.
        private const val SCORE_THRESHOLD = 0.30f
        private const val IOU_THRESHOLD = 0.45f
    }
}
