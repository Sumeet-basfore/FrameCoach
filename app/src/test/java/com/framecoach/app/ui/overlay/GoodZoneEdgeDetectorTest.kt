package com.framecoach.app.ui.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GoodZoneEdgeDetector].
 *
 * T8 acceptance criteria verified here:
 *   - Single distinct pulse on entering a good zone.
 *   - No repeat pulse while staying in the good zone.
 *   - Re-fires only after leaving and re-entering.
 *
 * Pure Kotlin — no Android framework, no device required.
 */
class GoodZoneEdgeDetectorTest {

    private lateinit var detector: GoodZoneEdgeDetector

    @Before
    fun setUp() {
        detector = GoodZoneEdgeDetector()
    }

    // -------------------------------------------------------------------------
    // Initial state (previouslyGood = true)
    // -------------------------------------------------------------------------

    @Test
    fun `first call with good=true does NOT fire — no spurious pulse on launch`() {
        // Camera just opened; composition is immediately "good". Should NOT pulse.
        assertFalse("No pulse expected on first-call good=true", detector.onUpdate(true))
    }

    @Test
    fun `first call with good=false does NOT fire`() {
        assertFalse("No pulse expected on first-call good=false", detector.onUpdate(false))
    }

    // -------------------------------------------------------------------------
    // Rising-edge detection (false → true)
    // -------------------------------------------------------------------------

    @Test
    fun `false then true fires exactly once`() {
        detector.onUpdate(false)                                  // enter not-good state
        assertTrue("Rising edge should fire", detector.onUpdate(true))
    }

    @Test
    fun `sustained good zone does NOT re-fire`() {
        detector.onUpdate(false)
        assertTrue("First entry should fire", detector.onUpdate(true))

        // Stay in good zone for many frames.
        repeat(20) { i ->
            assertFalse("No repeat pulse on sustained good (frame $i)", detector.onUpdate(true))
        }
    }

    @Test
    fun `leaving and re-entering fires again`() {
        // Enter good zone.
        detector.onUpdate(false)
        assertTrue("First entry", detector.onUpdate(true))

        // Leave good zone.
        detector.onUpdate(false)

        // Re-enter good zone — must fire again.
        assertTrue("Re-entry should fire", detector.onUpdate(true))
    }

    @Test
    fun `multiple leave-and-re-enter cycles each fire once`() {
        repeat(5) { cycle ->
            detector.onUpdate(false)  // leave / stay not-good
            assertTrue("Cycle $cycle entry should fire", detector.onUpdate(true))
            // Stay good for a few frames — no extra pulses.
            repeat(3) {
                assertFalse("No extra pulse in cycle $cycle", detector.onUpdate(true))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Falling edge (true → false) should NOT fire
    // -------------------------------------------------------------------------

    @Test
    fun `falling edge — good to not-good does NOT fire`() {
        detector.onUpdate(false) // seed as not-good first
        detector.onUpdate(true)  // enter good (fires)
        assertFalse("Falling edge must not fire", detector.onUpdate(false))
    }

    @Test
    fun `sustained not-good zone does NOT fire`() {
        repeat(20) { i ->
            assertFalse("Not-good frame $i should not fire", detector.onUpdate(false))
        }
    }

    // -------------------------------------------------------------------------
    // reset()
    // -------------------------------------------------------------------------

    @Test
    fun `reset then immediate good=true does NOT fire`() {
        // Drive the detector into not-good state first.
        detector.onUpdate(false)
        detector.reset()  // reset should set previouslyGood = true

        // After reset, the first "good" frame should not fire a spurious pulse.
        assertFalse("No pulse immediately after reset with good=true", detector.onUpdate(true))
    }

    @Test
    fun `reset then not-good then good fires normally`() {
        // Get to good state, then reset.
        detector.onUpdate(false)
        detector.onUpdate(true)
        detector.reset()

        // Normal cycle after reset.
        detector.onUpdate(false)
        assertTrue("Should fire after reset + not-good + good", detector.onUpdate(true))
    }
}
