package com.example.flora.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.MotionEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Renders a live CameraX back-camera preview inside an [AndroidView].
 *
 * @param onCameraReady Called (once) with the bound [ImageCapture] instance so the caller
 *                      can trigger photo capture whenever the shutter button is pressed.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (ImageCapture) -> Unit = {}
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // COMPATIBLE mode uses TextureView instead of SurfaceView.
    // TextureView respects the normal Android view z-order, so the Compose
    // navigation transition can properly cover it when leaving the camera screen,
    // eliminating the "camera bleeds through new page" artefact.
    @SuppressLint("ClickableViewAccessibility")
    val previewView   = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // Disable touch on preview so taps pass through to Compose shutter button
            setOnTouchListener { _, _ -> true }
        }
    }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var boundImageCapture: ImageCapture? = null

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            boundPreview = preview
            boundImageCapture = imageCapture

            try {
                // Unbind everything first so a previous screen's leftover use-cases
                // can't collide with ours. Safe because the previous screen's
                // onDispose targets only its own use-cases now.
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                onCameraReady(imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            // Only unbind OUR use-cases. Using unbindAll() here races with the next
            // screen's binding (async listener) and freezes its preview.
            val provider = cameraProvider
            val useCases = listOfNotNull(boundPreview, boundImageCapture).toTypedArray()
            if (provider != null && useCases.isNotEmpty()) {
                try { provider.unbind(*useCases) } catch (_: Exception) {}
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Capture a single JPEG from [imageCapture], auto-rotating it to match the sensor orientation.
 *
 * @param onResult Called with the resulting [Bitmap], or null on failure.
 */
fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onResult: (Bitmap?) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val matrix   = Matrix().apply { postRotate(rotation.toFloat()) }
                val bitmap   = Bitmap.createBitmap(
                    image.toBitmap(), 0, 0, image.width, image.height, matrix, true
                )
                image.close()
                onResult(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onResult(null)
            }
        }
    )
}
