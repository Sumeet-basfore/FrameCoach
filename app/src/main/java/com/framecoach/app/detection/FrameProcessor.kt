package com.framecoach.app.detection

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coroutine-based frame processor with adaptive frame-skip throttling (T7).
 *
 * On every call the [AdaptiveFrameGate] decides whether this frame should be
 * run through detection or dropped.  If dropped, the [ImageProxy] is closed
 * immediately and an empty list is returned — the overlay keeps its last known
 * state, which is imperceptible at normal camera frame-rates.
 *
 * When detection runs, wall-clock latency is measured and fed back to the gate
 * so the skip interval adapts: slow device → fewer detections per second;
 * fast device (or after a thermal event subsides) → more detections per second.
 *
 * Hard constraints satisfied:
 *  - Detection never blocks the preview thread (coroutine on Dispatchers.Default).
 *  - Frames are always closed (either in the skip branch or in the finally block).
 */
class FrameProcessor(private val context: Context) {

    companion object {
        private const val TAG = "FrameProcessor"
    }

    private var objectDetectorHelper: ObjectDetectorHelper? = null
    private var faceDetectorHelper: FaceDetectorHelper? = null

    @Synchronized
    private fun getObjectDetector(): ObjectDetectorHelper {
        val detector = objectDetectorHelper
        if (detector != null) return detector
        val newDetector = ObjectDetectorHelper(context)
        objectDetectorHelper = newDetector
        return newDetector
    }

    @Synchronized
    private fun getFaceDetector(): FaceDetectorHelper {
        val detector = faceDetectorHelper
        if (detector != null) return detector
        val newDetector = FaceDetectorHelper(context)
        faceDetectorHelper = newDetector
        return newDetector
    }

    /** Gate that decides which frames to process and adapts the skip interval. */
    internal val gate = AdaptiveFrameGate()

    private val thermalStatusListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            Log.d(TAG, "Thermal status changed: $status")
            gate.thermalStatus = status
        }
    } else {
        null
    }

    init {
        registerThermalStatusListener()
    }

    private fun registerThermalStatusListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalStatusListener != null) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.addThermalStatusListener(context.mainExecutor, thermalStatusListener)
                Log.d(TAG, "Successfully registered thermal status listener")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register thermal status listener", e)
            }
        }
    }

    private fun unregisterThermalStatusListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalStatusListener != null) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.removeThermalStatusListener(thermalStatusListener)
                Log.d(TAG, "Successfully unregistered thermal status listener")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister thermal status listener", e)
            }
        }
    }

    /**
     * Process a single camera frame, subject to adaptive throttling.
     *
     * If the gate decides this frame should be skipped, [imageProxy] is closed
     * immediately and an empty list is returned.  Otherwise detection runs on
     * [Dispatchers.Default] and the measured latency is fed back to the gate.
     *
     * @param imageProxy  Frame from CameraX ImageAnalysis.
     * @param activeMode  The active camera mode ("general" or "portrait").
     * @return            Detected bounding boxes, or empty if skipped / no detections.
     */
    suspend fun processFrame(imageProxy: ImageProxy, activeMode: String): List<BoundingBox> {
        // Ask the gate whether to process this frame.
        if (!gate.shouldProcess()) {
            imageProxy.close()
            return emptyList()
        }

        return withContext(Dispatchers.Default) {
            val startMs = System.currentTimeMillis()
            try {
                val boxes = if (activeMode == "portrait") {
                    getFaceDetector().detect(imageProxy)
                } else {
                    getObjectDetector().detect(imageProxy)
                }
                if (boxes.isNotEmpty()) {
                    Log.d(TAG, "Frame processed ($activeMode): ${boxes.size} detection(s)")
                }
                boxes
            } catch (e: Exception) {
                Log.e(TAG, "Detection error in mode $activeMode", e)
                emptyList()
            } finally {
                // Always close the proxy before recording latency so that we
                // measure the full cost (including any buffer release overhead).
                imageProxy.close()
                val elapsedMs = System.currentTimeMillis() - startMs
                gate.recordLatency(elapsedMs)
                Log.v(
                    TAG,
                    "Latency ${elapsedMs}ms | EMA ${"%.1f".format(gate.emaMs)}ms " +
                        "| skip=${gate.currentSkipInterval}",
                )
            }
        }
    }

    /**
     * Release all native resources and reset the gate.
     */
    @Synchronized
    fun close() {
        unregisterThermalStatusListener()
        objectDetectorHelper?.close()
        objectDetectorHelper = null
        faceDetectorHelper?.close()
        faceDetectorHelper = null
        gate.reset()
    }
}

