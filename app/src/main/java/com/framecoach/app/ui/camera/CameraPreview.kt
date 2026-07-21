package com.framecoach.app.ui.camera

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.framecoach.app.detection.ExposureAnalyzer
import com.framecoach.app.detection.ExposureState
import com.framecoach.app.detection.FrameProcessor
import com.framecoach.app.ui.overlay.CompositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberUpdatedState

/**
 * Camera preview composable with live object detection and image capture capability.
 *
 * Binds both [Preview] (for the viewfinder) and [ImageAnalysis] (for detection)
 * use cases to the current [LifecycleOwner]. Also binds [ImageCapture] for taking
 * full-resolution photos.
 *
 * Detection runs on a background coroutine dispatcher; results are posted to
 * [CompositionState] and consumed by [CompositionOverlay] in the parent screen.
 *
 * When [onImageCaptureChanged] is called with a non-null [ImageCapture] instance,
 * the parent can use it to take a picture.
 *
 * Lifecycle binding:
 *   [ProcessCameraProvider] is obtained once and bound. When this composable
 *   leaves composition, all use cases are unbound, in-flight coroutines are
 *   cancelled, and [FrameProcessor] is closed.
 *
 * @param lifecycleOwner The lifecycle scope to bind the camera to.
 * @param cameraMode    The active camera coaching mode ("general" or "portrait").
 * @param modifier      Modifier for the outer layout container.
 * @param onImageCaptureChanged Callback invoked when the [ImageCapture] use case is ready
 *                              (or null when the camera is closed).
 */
@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraMode: String,
    compositionStyle: String,
    sensitivity: Float = 0.42f,
    modifier: Modifier = Modifier,
    onImageCaptureChanged: ((ImageCapture?) -> Unit)? = null,
    onCameraReady: ((Camera) -> Unit)? = null,
) {
    val context = LocalContext.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    // T11: remember the updated cameraMode to avoid capturing stale values in the analyzer callback
    val currentMode = rememberUpdatedState(cameraMode)
    val currentStyle = rememberUpdatedState(compositionStyle)
    val currentSensitivity = rememberUpdatedState(sensitivity)

    // Coroutine scope with SupervisorJob — cancelled on dispose to drain in-flight work
    // before native resources are freed.
    val coroutineScope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // Remember the frame processor (tied to component lifetime).
    val frameProcessor = remember { FrameProcessor(context) }

    // T12: exposure analyzer
    val exposureAnalyzer = remember { ExposureAnalyzer() }

    // Remember the ImageCapture instance to pass back to the parent.
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Clean up: cancel scope FIRST so in-flight detect() calls finish before
    // close() frees the native detector.  Then close the processor and clear imageCapture.
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.cancel()
            frameProcessor.close()
            // Notify parent that the camera is closed.
            onImageCaptureChanged?.invoke(null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // --- Preview use case ---
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(surfaceProvider) }

                    // --- ImageAnalysis use case (T3) ---
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    // --- ImageCapture use case (T6) ---
                    val capturedImageCapture = ImageCapture.Builder()
                        .build()

                    // ImageProxy lifetime is managed here — close in finally so it is
                    // never leaked regardless of cancellation, exception, or success.
                    imageAnalysis.setAnalyzer(mainExecutor) { imageProxy: ImageProxy ->
                        // T12: extract Y plane for exposure analysis BEFORE processFrame
                        // closes the ImageProxy.
                        val yPlane = imageProxy.planes.getOrNull(0)
                        if (yPlane != null) {
                            val yBuffer = yPlane.buffer.duplicate()
                            yBuffer.rewind()
                            val exposureResult = exposureAnalyzer.analyse(
                                yBuffer, imageProxy.width, imageProxy.height
                            )
                            ExposureState.update(exposureResult)
                        }

                        val job = coroutineScope.launch {
                            try {
                                val boxes = frameProcessor.processFrame(imageProxy, currentMode.value)
                                // MutableStateFlow.value is thread-safe; no Main dispatcher needed.
                                CompositionState.update(boxes, currentStyle.value, currentMode.value, currentSensitivity.value)
                            } catch (e: Exception) {
                                // FrameProcessor.processFrame already closes the proxy in finally,
                                // but guard against any early-exit path.
                                imageProxy.close()
                            }
                        }
                        job.invokeOnCompletion { throwable ->
                            if (throwable != null) {
                                imageProxy.close()
                            }
                        }
                    }

                    // Unbind before rebinding.
                    cameraProvider.unbindAll()

                    // Bind all three use cases to lifecycle and get the Camera handle.
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                        capturedImageCapture
                    )

                    // Update the remembered imageCapture instance and notify parent.
                    imageCapture = capturedImageCapture
                    onImageCaptureChanged?.invoke(capturedImageCapture)
                    onCameraReady?.invoke(camera)
                }, mainExecutor)
            }
        },
    )
}