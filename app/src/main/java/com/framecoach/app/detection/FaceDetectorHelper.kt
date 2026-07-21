package com.framecoach.app.detection

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps MediaPipe's [FaceDetector] running BlazeFace.
 *
 * This class:
 * - Loads the TFLite model asset on creation.
 * - Exposes [detect] which accepts an [ImageProxy] frame and returns a list of
 *   normalised [BoundingBox]es.
 *
 * Thread safety: [FaceDetector] is NOT thread-safe, so all calls to [detect]
 * are serialised via a [Mutex].
 */
class FaceDetectorHelper(private val context: Context) {

    companion object {
        private const val TAG = "FaceDetectorHelper"
        private const val MODEL_PATH = "models/face_detector.tflite"
        private const val MIN_CONFIDENCE = 0.5f
    }

    private val detector: FaceDetector? = createDetector()

    // Serialise detector calls — FaceDetector is NOT thread-safe.
    private val detectorMutex = Mutex()

    private val closeLock = Any()
    @Volatile
    private var isClosed = false

    private fun createDetector(): FaceDetector? {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(MIN_CONFIDENCE)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            FaceDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create FaceDetector", e)
            null
        }
    }

    /**
     * Run detection on a single camera frame.
     *
     * Calls are serialised by an internal [Mutex].
     *
     * @param imageProxy The frame from CameraX ImageAnalysis.
     * @return List of detected faces, sorted by confidence descending.
     */
    suspend fun detect(imageProxy: ImageProxy): List<BoundingBox> = detectorMutex.withLock {
        val activeDetector = detector ?: return@withLock emptyList()
        synchronized(closeLock) {
            if (isClosed) return@withLock emptyList()
        }

        val mpImage = imageProxyToMPImage(imageProxy)
            ?: return@withLock emptyList()

        val result: FaceDetectorResult? = synchronized(closeLock) {
            if (isClosed) null else {
                try {
                    activeDetector.detect(mpImage)
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPipe face detection failed", e)
                    null
                }
            }
        }
        if (result == null) return@withLock emptyList()

        val boxes = mutableListOf<BoundingBox>()
        for (detection in result.detections()) {
            val categories = detection.categories()
            val score = if (categories.isNotEmpty()) categories[0].score() else 1.0f
            val label = if (categories.isNotEmpty() && categories[0].categoryName() != null) {
                categories[0].categoryName()
            } else {
                "face"
            }
            val bbox: RectF = detection.boundingBox()

            // ImageProxy dimensions are the analysis-frame dimensions.
            val frameW = imageProxy.width.toFloat()
            val frameH = imageProxy.height.toFloat()

            boxes.add(
                BoundingBox(
                    label = label,
                    confidence = score,
                    left = bbox.left / frameW,
                    top = bbox.top / frameH,
                    right = bbox.right / frameW,
                    bottom = bbox.bottom / frameH,
                )
            )
        }

        // Sort by confidence descending
        boxes.sortByDescending { it.confidence }

        if (boxes.isNotEmpty()) {
            Log.d(TAG, "Detected ${boxes.size} face(s): " +
                    boxes.joinToString { "${it.label} (${"%.2f".format(it.confidence)})" })
        }

        boxes
    }

    private fun imageProxyToMPImage(imageProxy: ImageProxy): MPImage? {
        return try {
            val bitmap = imageProxy.toBitmap()
            BitmapImageBuilder(bitmap).build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ImageProxy to MPImage", e)
            null
        }
    }

    fun close() {
        synchronized(closeLock) {
            if (!isClosed) {
                isClosed = true
                try {
                    detector?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing FaceDetector", e)
                }
            }
        }
    }
}
