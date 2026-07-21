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
        /** Set to true the first time the user dismisses the onboarding overlay (B1). */
        const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
        /** Audio coaching cues via on-device TTS (C1). Off by default. */
        const val KEY_AUDIO_COACHING_ENABLED = "audio_coaching_enabled"

        const val MODE_GENERAL = "general"
        const val MODE_PORTRAIT = "portrait"
        const val MODE_PRODUCT = "product"

        const val STYLE_RULE_OF_THIRDS = "rule_of_thirds"
        const val STYLE_GOLDEN_RATIO = "golden_ratio"
        const val STYLE_CENTER_GRID = "center_grid"

        const val KEY_SENSITIVITY = "sensitivity"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // -------------------------------------------------------------------------
    // Sensitivity Threshold
    // -------------------------------------------------------------------------

    private val _sensitivity = MutableStateFlow(prefs.getFloat(KEY_SENSITIVITY, 0.42f))

    /** Sensitivity threshold for composition coach. */
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    /** Persist and publish a new [sensitivity] value. */
    fun setSensitivity(value: Float) {
        prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()
        _sensitivity.value = value
    }

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

    // -------------------------------------------------------------------------
    // Onboarding (B1)
    // -------------------------------------------------------------------------

    private val _onboardingShown = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
    )

    /**
     * Whether the first-launch onboarding overlay has been dismissed at least once.
     * Defaults to false so new installs always see the tutorial.
     */
    val onboardingShown: StateFlow<Boolean> = _onboardingShown.asStateFlow()

    /** Persist that the onboarding overlay has been dismissed. */
    fun setOnboardingShown(shown: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, shown).apply()
        _onboardingShown.value = shown
    }

    // -------------------------------------------------------------------------
    // Audio coaching (C1)
    // -------------------------------------------------------------------------

    private val _audioCoachingEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUDIO_COACHING_ENABLED, false)   // off by default
    )

    /**
     * Whether audio coaching cues (TTS) are active.
     * Defaults to **false** — users opt in rather than being surprised by speech.
     */
    val audioCoachingEnabled: StateFlow<Boolean> = _audioCoachingEnabled.asStateFlow()

    /** Persist and publish a new [audioCoachingEnabled] value. */
    fun setAudioCoachingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_COACHING_ENABLED, enabled).apply()
        _audioCoachingEnabled.value = enabled
    }
}
