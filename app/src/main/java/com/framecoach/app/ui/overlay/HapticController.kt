package com.framecoach.app.ui.overlay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Fires a single short haptic pulse when the composition enters the "good zone" (T8).
 *
 * Delegates edge-detection to [GoodZoneEdgeDetector] so the pulse fires only on the
 * false → true transition of [isGood], satisfying the acceptance criterion:
 *
 *   "Single distinct pulse on entering a good zone; no repeat pulse until the
 *    frame leaves and re-enters that state."
 *
 * The vibration itself uses [VibrationEffect.EFFECT_CLICK] on API 29+, falling back
 * to a 30 ms legacy vibration on older devices. [VibrationEffect.EFFECT_CLICK] is a
 * system-defined, crisp single-tap pattern that feels intentional without being intrusive.
 *
 * @param context Application or Activity context — used to obtain the [Vibrator] service.
 */
class HapticController(context: Context) {

    companion object {
        private const val TAG = "HapticController"

        /** Duration used for the legacy vibrate() fallback on API < 29. */
        private const val LEGACY_VIBRATE_MS = 30L
    }

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to obtain Vibrator service", e)
        null
    }

    private val edgeDetector = GoodZoneEdgeDetector()

    /**
     * Call once per composition result.  If this is a rising edge (not-good → good),
     * a short click haptic is triggered; otherwise this is a no-op.
     *
     * @param isGood Latest [CompositionSuggestion.isGood] value.
     */
    fun onCompositionUpdate(isGood: Boolean) {
        if (!edgeDetector.onUpdate(isGood)) return

        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            Log.d(TAG, "Good zone entered — firing haptic pulse")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(LEGACY_VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(LEGACY_VIBRATE_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic pulse failed", e)
        }
    }

    /**
     * Reset edge-detector state — call when the camera is re-bound so the first
     * good-zone entry after a restart fires correctly.
     */
    fun reset() {
        edgeDetector.reset()
    }
}
