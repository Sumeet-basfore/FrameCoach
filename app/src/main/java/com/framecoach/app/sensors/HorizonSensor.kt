package com.framecoach.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Accelerometer-based horizon sensor for T9.
 *
 * Reads `TYPE_ACCELEROMETER` events, applies a low-pass filter to isolate the
 * gravity component, and publishes a roll angle (degrees) on [rollDeg].
 *
 * **Roll convention** (portrait orientation, camera facing away from user):
 *   - 0°  = device perfectly level
 *   - +°  = device tilted clockwise  (right side down)
 *   - -°  = device tilted counter-clockwise (left side down)
 *
 * Call [start] when the camera screen becomes active and [stop] when it leaves
 * — preferably in a Compose [DisposableEffect] so the listener is always
 * unregistered even if the composable is removed unexpectedly.
 *
 * If no accelerometer hardware is present (rare on phones, possible on some
 * tablets or emulators), [rollDeg] stays at 0 and a warning is logged.
 */
class HorizonSensor(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "HorizonSensor"

        /**
         * Low-pass filter coefficient.  0.8 = heavy smoothing (reacts slowly
         * to sudden jerks, tracks sustained tilts faithfully).
         * Range: 0 < α ≤ 1.  Larger = more raw / responsive.
         */
        private const val ALPHA = 0.8f
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Smoothed gravity vector [x, y, z]. */
    private val gravity = FloatArray(3) { 0f }

    private val _rollDeg = MutableStateFlow(0f)

    /**
     * Latest smoothed roll angle in degrees.
     * Emits 0 if no sensor hardware is available.
     */
    val rollDeg: StateFlow<Float> = _rollDeg.asStateFlow()

    /**
     * Register the sensor listener.  No-op (with a warning) if no accelerometer
     * is available on this device.
     */
    fun start() {
        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer found — horizon indicator will not function")
            return
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        Log.d(TAG, "Horizon sensor started")
    }

    /**
     * Unregister the sensor listener and release resources.
     * Safe to call even if [start] was never called.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Horizon sensor stopped")
    }

    // -------------------------------------------------------------------------
    // SensorEventListener
    // -------------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent) {
        // Low-pass filter: isolate gravity, reject linear-acceleration noise.
        gravity[0] = ALPHA * gravity[0] + (1f - ALPHA) * event.values[0]
        gravity[1] = ALPHA * gravity[1] + (1f - ALPHA) * event.values[1]
        gravity[2] = ALPHA * gravity[2] + (1f - ALPHA) * event.values[2]

        // Roll: angle of the gravity vector in the X-Z plane (portrait mode).
        // atan2(x, sqrt(y²+z²)) gives rotation around the camera's optical axis.
        // Result is in [-90°, +90°]; 0° = level.
        val roll = Math.toDegrees(
            atan2(
                gravity[0].toDouble(),
                sqrt(gravity[1].toDouble() * gravity[1] + gravity[2].toDouble() * gravity[2]),
            )
        ).toFloat()

        _rollDeg.value = roll
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for gravity-based horizon levelling.
    }
}
