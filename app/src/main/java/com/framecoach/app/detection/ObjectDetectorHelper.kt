package com.framecoach.app.detection

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.ByteBufferImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps MediaPipe's [ObjectDetector] running EfficientDet-Lite0.
 *
 * This class:
 * - Loads the TFLite model asset on creation.
 * - Exposes [detect] which accepts an [ImageProxy] frame and returns a list of
 *   normalised [BoundingBox]es.
 *
 * Thread safety: [ObjectDetector] is NOT thread-safe, so all calls to [detect]
 * are serialised via a [Mutex].  The detection runs on [Dispatchers.Default]
 * in [FrameProcessor].
 */
class ObjectDetectorHelper(private val context: Context) {

    companion object {
        private const val TAG = "ObjectDetectorHelper"
        private const val MODEL_PATH = "models/efficientdet_lite0.tflite"
        private const val SCORE_THRESHOLD = 0.5f
        private const val MAX_RESULTS = 3
    }

    private val detector: ObjectDetector = createDetector()

    // Serialise detector calls — ObjectDetector is NOT thread-safe.
    private val detectorMutex = Mutex()

    private val closeLock = Any()
    @Volatile
    private var isClosed = false

    private fun createDetector(): ObjectDetector {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            .build()

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setScoreThreshold(SCORE_THRESHOLD)
            .setMaxResults(MAX_RESULTS)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        return ObjectDetector.createFromOptions(context, options)
    }

    /**
     * Run detection on a single camera frame.
     *
     * Calls are serialised by an internal [Mutex] so the detector is never
     * accessed concurrently.
     *
     * @param imageProxy The frame from CameraX ImageAnalysis.
     * @return List of detected objects, sorted by confidence descending (highest first).
     */
    suspend fun detect(imageProxy: ImageProxy): List<BoundingBox> = detectorMutex.withLock {
        synchronized(closeLock) {
            if (isClosed) return@withLock emptyList()
        }

        val mpImage = imageProxyToMPImage(imageProxy)
            ?: return@withLock emptyList()

        val result: ObjectDetectionResult? = synchronized(closeLock) {
            if (isClosed) null else detector.detect(mpImage)
        }
        if (result == null) return@withLock emptyList()

        val boxes = mutableListOf<BoundingBox>()
        for (detection in result.detections()) {
            val categories = detection.categories()
            if (categories.isEmpty()) continue

            val topCategory = categories[0]
            val bbox: RectF = detection.boundingBox()

            // ImageProxy dimensions are the analysis-frame dimensions.
            val frameW = imageProxy.width.toFloat()
            val frameH = imageProxy.height.toFloat()

            boxes.add(
                BoundingBox(
                    label = topCategory.categoryName(),
                    confidence = topCategory.score(),
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
            Log.d(TAG, "Detected ${boxes.size} object(s): " +
                    boxes.joinToString { "${it.label} (${"%.2f".format(it.confidence)})" })
        }

        boxes
    }

    /**
     * Convert a CameraX [ImageProxy] to a MediaPipe [MPImage].
     *
     * MediaPipe's ObjectDetector accepts [MPImage] in NV21 format.
     * CameraX ImageAnalysis delivers YUV_420_888 which we convert to NV21.
     *
     * The buffer is allocated fresh per frame (direct ByteBuffer, no GC pressure).
     * Size is calculated using standard NV21 layout (no row-stride padding) so that
     * MediaPipe reads the correct pixel data.
     */
    private fun imageProxyToMPImage(imageProxy: ImageProxy): MPImage? {
        return try {
            val planes = imageProxy.planes
            if (planes.size != 3) {
                Log.w(TAG, "Unexpected number of planes: ${planes.size}")
                return null
            }

            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val yPixelStride = planes[0].pixelStride ?: 1
            val yRowStride = planes[0].rowStride
            val uPixelStride = planes[1].pixelStride ?: 1
            val uRowStride = planes[1].rowStride
            val vPixelStride = planes[2].pixelStride ?: 1
            val vRowStride = planes[2].rowStride

            val width = imageProxy.width
            val height = imageProxy.height

            // Standard NV21 size: Y = width*height, UV = 2 * ceil(w/2) * ceil(h/2)
            // No row-stride padding — MediaPipe expects this layout.
            val uvWidth = (width + 1) / 2
            val uvHeight = (height + 1) / 2
            val nv21Size = width * height + uvWidth * uvHeight * 2

            val nv21Buffer = java.nio.ByteBuffer.allocateDirect(nv21Size)
            nv21Buffer.order(java.nio.ByteOrder.nativeOrder())

            // Copy Y plane — may have padding (rowStride > width).
            for (row in 0 until height) {
                val srcPos = row * yRowStride
                yBuffer.position(srcPos)
                if (yPixelStride == 1 && yRowStride == width) {
                    // Contiguous row — copy in one call.
                    val chunk = ByteArray(width)
                    yBuffer.get(chunk)
                    nv21Buffer.put(chunk)
                } else {
                    // Strided or padded row — copy pixel by pixel.
                    for (col in 0 until width) {
                        yBuffer.position(srcPos + col * yPixelStride)
                        nv21Buffer.put(yBuffer.get())
                    }
                }
            }

            // Copy UV planes as interleaved VU (NV21: V first, then U).
            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val uPos = row * uRowStride + col * uPixelStride
                    val vPos = row * vRowStride + col * vPixelStride
                    uBuffer.position(uPos)
                    vBuffer.position(vPos)
                    nv21Buffer.put(vBuffer.get()) // V first
                    nv21Buffer.put(uBuffer.get()) // U second
                }
            }

            nv21Buffer.rewind()

            // ByteBufferImageBuilder(ByteBuffer, width, height, imageFormat)
            return ByteBufferImageBuilder(nv21Buffer, width, height, MPImage.IMAGE_FORMAT_NV21)
                .build()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ImageProxy to MPImage", e)
            null
        }
    }

    /**
     * Release the detector resources.
     */
    fun close() {
        synchronized(closeLock) {
            if (!isClosed) {
                isClosed = true
                detector.close()
            }
        }
    }
}