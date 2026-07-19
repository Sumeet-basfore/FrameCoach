package com.framecoach.app.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HorizonLevelCalculator].
 *
 * T9 acceptance criteria verified (testable portion — the sensor itself
 * requires hardware so it is excluded from unit tests):
 *   - Indicator disappears when level within threshold.
 *   - Indicator appears when tilted beyond threshold.
 *   - Normalised tilt is correct at key angles.
 *
 * Pure Kotlin — no Android framework, no device required.
 */
class HorizonLevelCalculatorTest {

    // -------------------------------------------------------------------------
    // isLevel flag
    // -------------------------------------------------------------------------

    @Test
    fun `exactly 0 degrees is level`() {
        val state = HorizonLevelCalculator.compute(0f)
        assertTrue("0° should be level", state.isLevel)
    }

    @Test
    fun `roll within positive threshold is level`() {
        val threshold = HorizonLevelCalculator.LEVEL_THRESHOLD_DEG
        val state = HorizonLevelCalculator.compute(threshold)
        assertTrue("+threshold should still be level (inclusive)", state.isLevel)
    }

    @Test
    fun `roll within negative threshold is level`() {
        val threshold = HorizonLevelCalculator.LEVEL_THRESHOLD_DEG
        val state = HorizonLevelCalculator.compute(-threshold)
        assertTrue("-threshold should still be level (inclusive)", state.isLevel)
    }

    @Test
    fun `roll just above positive threshold is NOT level`() {
        val threshold = HorizonLevelCalculator.LEVEL_THRESHOLD_DEG
        val state = HorizonLevelCalculator.compute(threshold + 0.01f)
        assertFalse("Slightly above +threshold should not be level", state.isLevel)
    }

    @Test
    fun `roll just below negative threshold is NOT level`() {
        val threshold = HorizonLevelCalculator.LEVEL_THRESHOLD_DEG
        val state = HorizonLevelCalculator.compute(-(threshold + 0.01f))
        assertFalse("Slightly below -threshold should not be level", state.isLevel)
    }

    @Test
    fun `large positive tilt is NOT level`() {
        val state = HorizonLevelCalculator.compute(45f)
        assertFalse("45° tilt should not be level", state.isLevel)
    }

    @Test
    fun `large negative tilt is NOT level`() {
        val state = HorizonLevelCalculator.compute(-45f)
        assertFalse("-45° tilt should not be level", state.isLevel)
    }

    // -------------------------------------------------------------------------
    // normalisedTilt
    // -------------------------------------------------------------------------

    @Test
    fun `0 degrees gives normalised tilt of 0`() {
        val state = HorizonLevelCalculator.compute(0f)
        assertEquals("0° → normalisedTilt = 0", 0f, state.normalisedTilt, 0.001f)
    }

    @Test
    fun `45 degrees gives normalised tilt of +1`() {
        val state = HorizonLevelCalculator.compute(45f)
        assertEquals("+45° → normalisedTilt = +1", 1f, state.normalisedTilt, 0.001f)
    }

    @Test
    fun `negative 45 degrees gives normalised tilt of -1`() {
        val state = HorizonLevelCalculator.compute(-45f)
        assertEquals("-45° → normalisedTilt = -1", -1f, state.normalisedTilt, 0.001f)
    }

    @Test
    fun `22_5 degrees gives normalised tilt of 0_5`() {
        val state = HorizonLevelCalculator.compute(22.5f)
        assertEquals("22.5° → normalisedTilt ≈ 0.5", 0.5f, state.normalisedTilt, 0.001f)
    }

    @Test
    fun `tilt beyond 45 degrees is clamped to 1`() {
        val state = HorizonLevelCalculator.compute(90f)
        assertEquals("90° should clamp normalisedTilt to 1", 1f, state.normalisedTilt, 0.001f)
    }

    @Test
    fun `tilt below minus 45 degrees is clamped to minus 1`() {
        val state = HorizonLevelCalculator.compute(-90f)
        assertEquals("-90° should clamp normalisedTilt to -1", -1f, state.normalisedTilt, 0.001f)
    }

    // -------------------------------------------------------------------------
    // rollDeg is preserved verbatim
    // -------------------------------------------------------------------------

    @Test
    fun `rollDeg is preserved in output`() {
        val input = 17.3f
        val state = HorizonLevelCalculator.compute(input)
        assertEquals("rollDeg should be preserved", input, state.rollDeg, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Custom threshold parameter
    // -------------------------------------------------------------------------

    @Test
    fun `custom threshold is respected`() {
        val state = HorizonLevelCalculator.compute(rollDeg = 5f, threshold = 10f)
        assertTrue("5° should be level with a 10° threshold", state.isLevel)
    }

    @Test
    fun `custom threshold not level when exceeded`() {
        val state = HorizonLevelCalculator.compute(rollDeg = 11f, threshold = 10f)
        assertFalse("11° should NOT be level with a 10° threshold", state.isLevel)
    }
}
