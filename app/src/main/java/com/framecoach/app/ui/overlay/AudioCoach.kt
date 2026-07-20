package com.framecoach.app.ui.overlay

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.framecoach.app.rules.Direction
import java.util.Locale

/**
 * Audio coaching cues using Android's on-device [TextToSpeech] engine (C1).
 *
 * Speaks a cue only when the [Direction] changes — never every frame.
 * This mirrors the edge-detection pattern used by [GoodZoneEdgeDetector] for haptics.
 *
 * Design decisions:
 * - Uses QUEUE_FLUSH so a new cue immediately replaces a stale one in the queue.
 * - The "good" state is spoken once on entry (same edge-fire rule as haptics).
 * - No network call — relies exclusively on the device TTS engine.
 * - Initialisation is async; cues are silently dropped if the engine is not ready yet.
 *
 * Lifecycle: call [init] (suspending init), [speak] on each update, [shutdown] on dispose.
 */
class AudioCoach(private val context: Context) {

    companion object {
        private const val TAG = "AudioCoach"
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    /** Last direction that was spoken — used to suppress repeated identical cues. */
    private var lastSpokenDirection: Direction? = null

    /**
     * Initialise the TTS engine. Safe to call multiple times; subsequent calls are no-ops
     * once the engine is already ready.
     *
     * [onReady] is invoked on the TTS init thread when the engine is available.
     */
    fun init(onReady: () -> Unit = {}) {
        if (ttsReady) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
                Log.d(TAG, "TTS engine ready")
                onReady()
            } else {
                Log.w(TAG, "TTS initialisation failed with status: $status")
            }
        }
    }

    /**
     * Speak a cue for [direction], but only if it differs from the last spoken direction.
     * Call this on every composition update; the internal guard ensures it fires at most
     * once per direction-change edge.
     *
     * @param direction  The current [Direction] from the rules engine.
     * @param enabled    Whether audio coaching is currently enabled (from [AppPreferences]).
     */
    fun onDirectionUpdate(direction: Direction, enabled: Boolean) {
        if (!enabled || !ttsReady) return
        if (direction == lastSpokenDirection) return          // same direction — suppress
        lastSpokenDirection = direction
        val cue = directionCue(direction)
        tts?.speak(cue, TextToSpeech.QUEUE_FLUSH, null, direction.name)
        Log.d(TAG, "TTS spoke: \"$cue\"")
    }

    /**
     * Reset spoken-direction state. Call when coaching is toggled off and back on so
     * the first update after re-enabling always fires.
     */
    fun reset() {
        lastSpokenDirection = null
    }

    /** Release TTS resources. Call from a [DisposableEffect] onDispose. */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        lastSpokenDirection = null
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a [Direction] to a short, natural-language coaching cue.
     * Kept simple — no arrows, no jargon, clear spatial instruction.
     */
    internal fun directionCue(direction: Direction): String = when (direction) {
        Direction.LEFT   -> "Move left"
        Direction.RIGHT  -> "Move right"
        Direction.UP     -> "Move up"
        Direction.DOWN   -> "Move down"
        Direction.CLOSER -> "Get closer"
        Direction.AWAY   -> "Step back"
        Direction.NONE   -> "Good composition"
    }
}
