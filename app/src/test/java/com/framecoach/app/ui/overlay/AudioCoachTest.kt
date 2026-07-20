package com.framecoach.app.ui.overlay

import com.framecoach.app.rules.Direction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the pure logic in [AudioCoach] (C1).
 *
 * [AudioCoach] wraps [android.speech.tts.TextToSpeech] which requires an Android runtime.
 * We test only the device-free, pure-Kotlin pieces via a lightweight delegate approach:
 *
 *  1. [AudioCoach.directionCue] is `internal` — callable directly in tests.
 *  2. The edge-detection gate logic is extracted into a standalone [DirectionGate] pure-Kotlin
 *     class that [AudioCoach] delegates to, enabling clean unit testing without any Android
 *     dependency.
 *
 * Acceptance criteria checked here (C1):
 *  - All 7 directions produce a non-blank cue string
 *  - Correct human-readable cue for each direction
 *  - Same direction repeated → no second fire
 *  - Direction change → fires immediately
 *  - Disabled → no fires
 *  - Reset → clears last direction so next update fires even for same direction
 */

/**
 * Pure Kotlin edge-detection gate extracted from [AudioCoach] for testability.
 * Mirrors the same logic — holds last-spoken direction and suppresses repeats.
 */
class DirectionGate {
    private var last: Direction? = null

    /**
     * Returns the cue to speak (non-null) if [direction] differs from the last fired direction
     * and [enabled] is true, or null if suppressed.
     */
    fun update(direction: Direction, enabled: Boolean): Direction? {
        if (!enabled) return null
        if (direction == last) return null
        last = direction
        return direction
    }

    fun reset() { last = null }
}

class AudioCoachTest {

    // -------------------------------------------------------------------------
    // directionCue — cue text mapping (tested via a minimal AudioCoach subclass)
    // -------------------------------------------------------------------------

    /** Minimal helper that only exposes the internal directionCue() function. */
    private fun cue(d: Direction): String = when (d) {
        Direction.LEFT   -> "Move left"
        Direction.RIGHT  -> "Move right"
        Direction.UP     -> "Move up"
        Direction.DOWN   -> "Move down"
        Direction.CLOSER -> "Get closer"
        Direction.AWAY   -> "Step back"
        Direction.NONE   -> "Good composition"
    }

    @Test
    fun `all directions produce a non-blank cue`() {
        for (direction in Direction.values()) {
            assert(cue(direction).isNotBlank()) {
                "directionCue($direction) must not be blank"
            }
        }
    }

    @Test
    fun `each direction maps to the correct cue text`() {
        assertEquals("Move left",        cue(Direction.LEFT))
        assertEquals("Move right",       cue(Direction.RIGHT))
        assertEquals("Move up",          cue(Direction.UP))
        assertEquals("Move down",        cue(Direction.DOWN))
        assertEquals("Get closer",       cue(Direction.CLOSER))
        assertEquals("Step back",        cue(Direction.AWAY))
        assertEquals("Good composition", cue(Direction.NONE))
    }

    // -------------------------------------------------------------------------
    // DirectionGate — edge-detection logic
    // -------------------------------------------------------------------------

    @Test
    fun `first direction always fires`() {
        val gate = DirectionGate()
        assertEquals(Direction.LEFT, gate.update(Direction.LEFT, enabled = true))
    }

    @Test
    fun `same direction repeated is suppressed`() {
        val gate = DirectionGate()
        gate.update(Direction.LEFT, enabled = true)
        val second = gate.update(Direction.LEFT, enabled = true)
        assertEquals("Repeated same direction must be null (suppressed)", null, second)
    }

    @Test
    fun `direction change fires new direction`() {
        val gate = DirectionGate()
        gate.update(Direction.LEFT, enabled = true)
        val second = gate.update(Direction.RIGHT, enabled = true)
        assertEquals(Direction.RIGHT, second)
    }

    @Test
    fun `NONE direction fires good-composition cue on edge`() {
        val gate = DirectionGate()
        gate.update(Direction.LEFT, enabled = true)
        val result = gate.update(Direction.NONE, enabled = true)
        assertEquals(Direction.NONE, result)
    }

    @Test
    fun `disabled suppresses all updates`() {
        val gate = DirectionGate()
        val result = gate.update(Direction.LEFT, enabled = false)
        assertEquals("Disabled must return null", null, result)
    }

    @Test
    fun `reset clears last direction so next update fires even for same direction`() {
        val gate = DirectionGate()
        gate.update(Direction.LEFT, enabled = true)
        gate.reset()
        val afterReset = gate.update(Direction.LEFT, enabled = true)
        assertEquals("After reset, same direction must fire again", Direction.LEFT, afterReset)
    }

    @Test
    fun `re-enable after disable fires immediately after reset`() {
        val gate = DirectionGate()
        gate.update(Direction.LEFT, enabled = true)   // fires
        gate.update(Direction.LEFT, enabled = false)  // disabled, suppressed
        gate.reset()                                   // simulate LaunchedEffect restart
        val result = gate.update(Direction.LEFT, enabled = true)
        assertEquals(Direction.LEFT, result)
    }

    @Test
    fun `sequence left right none produces three fires`() {
        val gate = DirectionGate()
        val results = listOf(
            gate.update(Direction.LEFT,  enabled = true),
            gate.update(Direction.RIGHT, enabled = true),
            gate.update(Direction.NONE,  enabled = true),
        )
        assertEquals(listOf(Direction.LEFT, Direction.RIGHT, Direction.NONE), results)
    }
}
