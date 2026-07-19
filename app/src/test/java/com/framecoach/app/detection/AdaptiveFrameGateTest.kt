package com.framecoach.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AdaptiveFrameGate].
 *
 * T7 acceptance: the gate must correctly skip frames, adapt the skip interval
 * upward under high latency, and recover it downward under low latency.
 *
 * All tests use pure Kotlin with no Android framework dependencies.
 */
class AdaptiveFrameGateTest {

    private lateinit var gate: AdaptiveFrameGate

    @Before
    fun setUp() {
        gate = AdaptiveFrameGate()
    }

    // -------------------------------------------------------------------------
    // shouldProcess() — frame-skip counter behaviour
    // -------------------------------------------------------------------------

    @Test
    fun `initial state — every frame is processed`() {
        // With skip interval = 1 (default), every frame should be processed.
        repeat(10) { i ->
            assertTrue("Frame $i should be processed at interval 1", gate.shouldProcess())
        }
    }

    @Test
    fun `skip interval 2 — every other frame is processed`() {
        // Manually set skip interval to 2 by using internal state via reset + recordLatency trick.
        // Easiest: directly test the counter arithmetic — frame 0 processed, frame 1 skipped.
        val gate2 = AdaptiveFrameGate(slowThresholdMs = 0L) // 0 threshold → first latency record raises interval
        gate2.shouldProcess()              // frame 0 — processed (interval still 1 at this point)
        gate2.recordLatency(100L)          // 100 ms >> threshold 0 → interval becomes 2
        assertEquals(2, gate2.currentSkipInterval)

        // Now frame 1 (counter=1): 1 % 2 != 0 → skip
        assertFalse("Frame 1 should be skipped at interval 2", gate2.shouldProcess())
        // Frame 2 (counter=2): 2 % 2 == 0 → process
        assertTrue("Frame 2 should be processed at interval 2", gate2.shouldProcess())
        // Frame 3 (counter=3): 3 % 2 != 0 → skip
        assertFalse("Frame 3 should be skipped at interval 2", gate2.shouldProcess())
    }

    @Test
    fun `skip interval 3 — one in three frames is processed`() {
        val gate3 = AdaptiveFrameGate(slowThresholdMs = 0L)
        gate3.shouldProcess()           // frame 0 — processed, triggers first latency
        gate3.recordLatency(200L)       // interval → 2
        gate3.shouldProcess()           // frame 1 — processed at interval 2
        gate3.recordLatency(200L)       // interval → 3

        assertEquals(3, gate3.currentSkipInterval)

        // frame 2: 2 % 3 != 0 → skip; frame 3: 3 % 3 == 0 → process; frame 4: 4 % 3 != 0 → skip
        assertFalse("Frame 2 should be skipped at interval 3", gate3.shouldProcess())
        assertTrue("Frame 3 should be processed at interval 3", gate3.shouldProcess())
        assertFalse("Frame 4 should be skipped at interval 3", gate3.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // recordLatency() — EMA and skip-interval adaptation
    // -------------------------------------------------------------------------

    @Test
    fun `first latency sample seeds the EMA directly`() {
        gate.shouldProcess()
        gate.recordLatency(60L)
        assertEquals(60.0, gate.emaMs, 0.001)
    }

    @Test
    fun `slow latency increases skip interval`() {
        val slowGate = AdaptiveFrameGate(slowThresholdMs = 50L)
        slowGate.shouldProcess()
        slowGate.recordLatency(200L) // Well above threshold

        assertTrue(
            "Skip interval should increase when latency exceeds slow threshold",
            slowGate.currentSkipInterval > AdaptiveFrameGate.MIN_SKIP_INTERVAL,
        )
    }

    @Test
    fun `fast latency decreases skip interval`() {
        // Start at a high skip interval, then report fast latency.
        val fastGate = AdaptiveFrameGate(slowThresholdMs = 50L, fastThresholdMs = 100L)
        // Drive interval up first.
        repeat(6) {
            fastGate.shouldProcess()
            fastGate.recordLatency(200L)
        }
        val intervalAfterSlowPhase = fastGate.currentSkipInterval
        assertTrue("Interval should have grown after slow latency", intervalAfterSlowPhase > 1)

        // Seed the EMA with fast latency to drive it below fastThresholdMs.
        // With alpha=0.2 and repeated 30-ms samples it will eventually cross 100 ms threshold.
        repeat(30) {
            fastGate.shouldProcess()
            fastGate.recordLatency(30L)
        }

        assertTrue(
            "Skip interval should decrease when latency drops below fast threshold",
            fastGate.currentSkipInterval < intervalAfterSlowPhase,
        )
    }

    @Test
    fun `skip interval is clamped to MAX_SKIP_INTERVAL`() {
        val clampGate = AdaptiveFrameGate(slowThresholdMs = 0L)
        // Record high latency many more times than MAX_SKIP_INTERVAL.
        repeat(AdaptiveFrameGate.MAX_SKIP_INTERVAL + 20) {
            clampGate.shouldProcess()
            clampGate.recordLatency(9999L)
        }
        assertEquals(
            "Skip interval must not exceed MAX_SKIP_INTERVAL",
            AdaptiveFrameGate.MAX_SKIP_INTERVAL,
            clampGate.currentSkipInterval,
        )
    }

    @Test
    fun `skip interval never drops below MIN_SKIP_INTERVAL`() {
        val floorGate = AdaptiveFrameGate(fastThresholdMs = Long.MAX_VALUE)
        repeat(20) {
            floorGate.shouldProcess()
            floorGate.recordLatency(0L)
        }
        assertEquals(
            "Skip interval must not go below MIN_SKIP_INTERVAL",
            AdaptiveFrameGate.MIN_SKIP_INTERVAL,
            floorGate.currentSkipInterval,
        )
    }

    @Test
    fun `EMA smooths over spikes — single spike does not immediately max out interval`() {
        // Alpha = 0.2; one spike at 9999 ms will only move the EMA by ~20%.
        // Seed EMA near 0 first, then inject one spike.
        gate.shouldProcess()
        gate.recordLatency(10L) // EMA = 10
        gate.shouldProcess()
        gate.recordLatency(9999L) // EMA ≈ 0.2 * 9999 + 0.8 * 10 ≈ 2007.8

        // Even though 2007 ms >> SLOW_THRESHOLD_MS, interval only increases by 1 per sample.
        // At this point interval should be 2, not MAX.
        assertTrue(
            "A single spike should only increment the interval by 1",
            gate.currentSkipInterval < AdaptiveFrameGate.MAX_SKIP_INTERVAL,
        )
    }

    // -------------------------------------------------------------------------
    // reset()
    // -------------------------------------------------------------------------

    @Test
    fun `reset restores initial state`() {
        // Mutate the gate.
        repeat(5) {
            gate.shouldProcess()
            gate.recordLatency(200L)
        }

        gate.reset()

        assertEquals("EMA should be 0 after reset", 0.0, gate.emaMs, 0.001)
        assertEquals(
            "Skip interval should be MIN after reset",
            AdaptiveFrameGate.MIN_SKIP_INTERVAL,
            gate.currentSkipInterval,
        )
        // After reset, the very first frame should be processed.
        assertTrue("First frame after reset should be processed", gate.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // Thermal Throttling
    // -------------------------------------------------------------------------

    @Test
    fun `severe thermal status immediately forces MAX_SKIP_INTERVAL`() {
        gate.thermalStatus = 3 // SEVERE
        assertEquals(AdaptiveFrameGate.MAX_SKIP_INTERVAL, gate.currentSkipInterval)
    }

    @Test
    fun `light thermal status immediately raises skip interval to at least 3`() {
        gate.thermalStatus = 1 // LIGHT
        assertTrue(gate.currentSkipInterval >= 3)
    }

    @Test
    fun `thermal throttling overrides recordLatency`() {
        gate.thermalStatus = 3
        gate.shouldProcess()
        gate.recordLatency(10L) // low latency should normally decrease skip interval, but thermal override blocks it
        assertEquals(AdaptiveFrameGate.MAX_SKIP_INTERVAL, gate.currentSkipInterval)
    }

    @Test
    fun `recovery of skip interval after thermal status returns to none`() {
        gate.thermalStatus = 3
        assertEquals(AdaptiveFrameGate.MAX_SKIP_INTERVAL, gate.currentSkipInterval)

        gate.thermalStatus = 0 // NONE
        // It shouldn't immediately jump to 1, but keep the current skip interval of 8.
        assertEquals(AdaptiveFrameGate.MAX_SKIP_INTERVAL, gate.currentSkipInterval)

        // Low latency records should now gradually decrease it.
        val initialInterval = gate.currentSkipInterval
        repeat(30) {
            gate.shouldProcess()
            gate.recordLatency(10L) // below fastThresholdMs (40L)
        }
        assertTrue("Skip interval should recover to a lower value", gate.currentSkipInterval < initialInterval)
    }
}
