package com.framecoach.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Unit tests for [ExposureAnalyzer] (T12).
 *
 * All tests construct synthetic Y-plane buffers so they run on JVM with
 * zero Android dependencies.
 */
class ExposureAnalyzerTest {

    private val analyzer = ExposureAnalyzer()
    private val W = 640
    private val H = 480

    @Test
    fun `well-exposed mid-grey frame produces no warning`() {
        val buf = uniformLuminanceBuffer(128, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertEquals(128f / 255f, result.meanLuminance, 0.01f)
        assertFalse(result.isWarning)
    }

    @Test
    fun `pure black frame is underexposed`() {
        val buf = uniformLuminanceBuffer(0, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertTrue(result.isUnderexposed)
        assertFalse(result.isOverexposed)
        assertTrue(result.isWarning)
    }

    @Test
    fun `pure white frame is overexposed`() {
        val buf = uniformLuminanceBuffer(255, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertTrue(result.isOverexposed)
        assertFalse(result.isUnderexposed)
        assertTrue(result.isWarning)
    }

    @Test
    fun `just below underexposed threshold`() {
        val luma = (ExposureAnalyzer.UNDEREXPOSED_THRESHOLD * 255f).toInt() - 1
        val buf = uniformLuminanceBuffer(luma.coerceAtLeast(0), W, H)
        val result = analyzer.analyse(buf, W, H)
        assertTrue(result.isUnderexposed)
    }

    @Test
    fun `just above underexposed threshold is not a warning`() {
        val luma = (ExposureAnalyzer.UNDEREXPOSED_THRESHOLD * 255f + 1).toInt()
        val buf = uniformLuminanceBuffer(luma, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertFalse(result.isUnderexposed)
        assertFalse(result.isOverexposed)
        assertFalse(result.isWarning)
    }

    @Test
    fun `just below overexposed threshold is not a warning`() {
        val luma = (ExposureAnalyzer.OVEREXPOSED_THRESHOLD * 255f - 1).toInt()
        val buf = uniformLuminanceBuffer(luma, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertFalse(result.isOverexposed)
        assertFalse(result.isWarning)
    }

    @Test
    fun `just over overexposed threshold`() {
        val luma = (ExposureAnalyzer.OVEREXPOSED_THRESHOLD * 255f + 1).toInt().coerceAtMost(255)
        val buf = uniformLuminanceBuffer(luma, W, H)
        val result = analyzer.analyse(buf, W, H)
        assertTrue(result.isOverexposed)
    }

    @Test
    fun `small frame is handled without crash`() {
        val buf = uniformLuminanceBuffer(128, 16, 16)
        val result = analyzer.analyse(buf, 16, 16)
        assertFalse(result.isWarning)
    }

    @Test
    fun `asymmetric buffer (tall narrow) is handled`() {
        val buf = uniformLuminanceBuffer(230, 100, 600)
        val result = analyzer.analyse(buf, 100, 600)
        assertTrue(result.isOverexposed)
    }

    @Test
    fun `checkerboard pattern produces mid-range mean`() {
        val buf = checkerboardBuffer(W, H)
        val result = analyzer.analyse(buf, W, H)
        // 50% black + 50% white = 0.5 luminance
        assertEquals(0.5f, result.meanLuminance, 0.05f)
        assertFalse(result.isWarning)
    }

    @Test
    fun `bright background with dark subject triggers backlight warning`() {
        val size = W * H
        val buf = ByteBuffer.allocateDirect(size)
        val xMinInner = W * 0.25
        val xMaxInner = W * 0.75
        val yMinInner = H * 0.25
        val yMaxInner = H * 0.75

        for (y in 0 until H) {
            for (x in 0 until W) {
                val isInner = x >= xMinInner && x <= xMaxInner && y >= yMinInner && y <= yMaxInner
                buf.put(if (isInner) 20.toByte() else 230.toByte())
            }
        }
        buf.rewind()
        val result = analyzer.analyse(buf, W, H)
        assertTrue(result.isBacklit)
        assertTrue(result.isWarning)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun uniformLuminanceBuffer(luma: Int, w: Int, h: Int): ByteBuffer {
        val size = w * h
        val buf = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) {
            buf.put(luma.toByte())
        }
        buf.rewind()
        return buf
    }

    private fun checkerboardBuffer(w: Int, h: Int): ByteBuffer {
        val size = w * h
        val buf = ByteBuffer.allocateDirect(size)
        // 4×4 checkerboard cells
        for (y in 0 until h) {
            for (x in 0 until w) {
                val isWhite = ((x / 4) + (y / 4)) % 2 == 0
                buf.put(if (isWhite) 255.toByte() else 0.toByte())
            }
        }
        buf.rewind()
        return buf
    }
}
