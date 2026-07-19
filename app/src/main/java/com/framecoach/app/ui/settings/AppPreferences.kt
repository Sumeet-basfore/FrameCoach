package com.framecoach.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin SharedPreferences wrapper that exposes settings as [StateFlow]s (T10).
 *
 * Keys and defaults:
 *   - [gridEnabled]    — controls the rule-of-thirds grid overlay; default **true**.
 *   - [hapticsEnabled] — controls haptic "good zone" pulse (T8); default **true**.
 *
 * Writes are committed via `apply()` (async, non-blocking), which is appropriate
 * for booleans that survive process death via ANR-safe background writes.
 *
 * One instance should be created with [remember] in the composition root and
 * passed down to consumers; no singleton pattern is needed at this scale.
 */
class AppPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "framecoach_prefs"
        const val KEY_GRID_ENABLED = "grid_enabled"
        const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        const val KEY_CAMERA_MODE = "camera_mode"
        const val KEY_COMPOSITION_STYLE = "composition_style"

        const val MODE_GENERAL = "general"
        const val MODE_PORTRAIT = "portrait"
        const val MODE_PRODUCT = "product"

        const val STYLE_RULE_OF_THIRDS = "rule_of_thirds"
        const val STYLE_GOLDEN_RATIO = "golden_ratio"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // -------------------------------------------------------------------------
    // Grid overlay
    // -------------------------------------------------------------------------

    private val _gridEnabled = MutableStateFlow(prefs.getBoolean(KEY_GRID_ENABLED, true))

    /** Whether the rule-of-thirds grid overlay is visible. */
    val gridEnabled: StateFlow<Boolean> = _gridEnabled.asStateFlow()

    /** Persist and publish a new [gridEnabled] value. */
    fun setGridEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GRID_ENABLED, enabled).apply()
        _gridEnabled.value = enabled
    }

    // -------------------------------------------------------------------------
    // Haptic feedback
    // -------------------------------------------------------------------------

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))

    /** Whether the haptic "good zone" pulse is active. */
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    /** Persist and publish a new [hapticsEnabled] value. */
    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    // -------------------------------------------------------------------------
    // Camera mode selection (General vs. Portrait)
    // -------------------------------------------------------------------------

    private val _cameraMode = MutableStateFlow(prefs.getString(KEY_CAMERA_MODE, MODE_GENERAL) ?: MODE_GENERAL)

    /** Active camera composition coaching mode. */
    val cameraMode: StateFlow<String> = _cameraMode.asStateFlow()

    /** Persist and publish a new [cameraMode] value. */
    fun setCameraMode(mode: String) {
        prefs.edit().putString(KEY_CAMERA_MODE, mode).apply()
        _cameraMode.value = mode
    }

    // -------------------------------------------------------------------------
    // Composition Style (Rule of Thirds vs. Golden Ratio)
    // -------------------------------------------------------------------------

    private val _compositionStyle = MutableStateFlow(
        prefs.getString(KEY_COMPOSITION_STYLE, STYLE_RULE_OF_THIRDS) ?: STYLE_RULE_OF_THIRDS
    )

    /** Active composition coaching style. */
    val compositionStyle: StateFlow<String> = _compositionStyle.asStateFlow()

    /** Persist and publish a new [compositionStyle] value. */
    fun setCompositionStyle(style: String) {
        prefs.edit().putString(KEY_COMPOSITION_STYLE, style).apply()
        _compositionStyle.value = style
    }
}
