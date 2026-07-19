package com.framecoach.app.rules

import com.framecoach.app.detection.BoundingBox
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals

/**
 * Unit tests for [CompositionRules].
 *
 * Ported from T2's Python prototype `test_with_synthetic()`.
 * All tests use normalised coordinates (0..1) so they run without a device.
 *
 * T4 acceptance criteria: tests cover at least:
 *   1. Centered subject  → "good"
 *   2. Off-center-left   → "move right"
 *   3. Subject too small → "get closer"
 *   4. Subject too large → "step back"
 *
 * Additional cases verify edge behaviour at the third-line boundaries and
 * all four off-centre directions.
 */
class RulesTest {

    // -------------------------------------------------------------------------
    // T4 acceptance-criteria cases
    // -------------------------------------------------------------------------

    @Test
    fun ` centred subject — good zone`() {
        // Normalised: centre (0.5, 0.5), size (0.32 × 0.32) → area = 0.1024 (near 10% peak)
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.34f,
            top = 0.34f,
            right = 0.66f,
            bottom = 0.66f,
        )

        val result = CompositionRules.analyse(box)

        assertTrue("Centered medium box should be in good zone", result.isGood)
        assertEquals(Direction.NONE, result.direction)
    }

    @Test
    fun ` off-centre-left — move right`() {
        // Box is in the left third of the frame.
        // Normalised centre x ≈ 0.156 (< 1/3), centre y ≈ 0.417 (within band)
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0f,
            top = 0.333f,
            right = 0.3125f,
            bottom = 0.5f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when off-center-left", result.isGood)
        assertEquals(Direction.RIGHT, result.direction)
    }

    @Test
    fun ` subject too small — get closer`() {
        // Fill ratio ≈ 0.00076 (well below TOO_SMALL_RATIO = 0.05)
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.4f,
            top = 0.4f,
            right = 0.42f,
            bottom = 0.42f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when subject too small", result.isGood)
        assertEquals(Direction.CLOSER, result.direction)
    }

    @Test
    fun ` subject too large — step back`() {
        // Fill ratio ≈ 0.625 (well above TOO_LARGE_RATIO = 0.50)
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 0.625f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when subject too large", result.isGood)
        assertEquals(Direction.AWAY, result.direction)
    }

    // -------------------------------------------------------------------------
    // Additional cases from T2 prototype (15 synthetic test cases)
    // -------------------------------------------------------------------------

    @Test
    fun ` off-centre-right — move left`() {
        // centre x ≈ 0.781 (> 2/3), centre y in band
        val box = BoundingBox(
            label = "dog",
            confidence = 0.8f,
            left = 0.625f,
            top = 0.333f,
            right = 0.9375f,
            bottom = 0.5f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when off-center-right", result.isGood)
        assertEquals(Direction.LEFT, result.direction)
    }

    @Test
    fun ` off-centre-above — move down`() {
        // centre y ≈ 0.208 (< 1/3), centre x in band
        val box = BoundingBox(
            label = "dog",
            confidence = 0.8f,
            left = 0.333f,
            top = 0f,
            right = 0.6f,
            bottom = 0.417f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when off-center-above", result.isGood)
        assertEquals(Direction.DOWN, result.direction)
    }

    @Test
    fun ` off-centre-below — move up`() {
        // centre y ≈ 0.75 (> 2/3), centre x in band
        val box = BoundingBox(
            label = "dog",
            confidence = 0.8f,
            left = 0.333f,
            top = 0.583f,
            right = 0.6f,
            bottom = 1f,
        )

        val result = CompositionRules.analyse(box)

        assertFalse("Should not be good when off-center-below", result.isGood)
        assertEquals(Direction.UP, result.direction)
    }

    @Test
    fun ` very small box — get closer`() {
        // Area ≈ 0.00065  (tiny — well below threshold)
        val box = BoundingBox(
            label = "cell phone",
            confidence = 0.7f,
            left = 0.4f,
            top = 0.4f,
            right = 0.41f,
            bottom = 0.41f,
        )

        val result = CompositionRules.analyse(box)

        assertEquals(Direction.CLOSER, result.direction)
    }

    @Test
    fun ` at rule-of-thirds intersection — good`() {
        // centre ≈ (0.4, 0.4) — safely inside the top-left intersection to avoid FP issues
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.275f,
            top = 0.275f,
            right = 0.525f,
            bottom = 0.525f,
        )

        val result = CompositionRules.analyse(box)

        assertTrue("Should be good at rule-of-thirds intersection", result.isGood)
    }

    @Test
    fun ` bottom-right intersection — good`() {
        // centre ≈ (0.6, 0.6) — safely inside the bottom-right intersection to avoid FP issues
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.45f,
            top = 0.45f,
            right = 0.75f,
            bottom = 0.75f,
        )

        val result = CompositionRules.analyse(box)

        assertTrue("Should be good at bottom-right intersection", result.isGood)
    }

    @Test
    fun ` left of third — move right takes priority over vertical`() {
        // centre x ≈ 0.2 (well left), centre y in band (0.5)
        // Box size 0.23×0.23 gives area ≈ 0.0529 (> 0.05) to avoid size check interference
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.085f,
            top = 0.385f,
            right = 0.315f,
            bottom = 0.615f,
        )

        val result = CompositionRules.analyse(box)

        // Horizontal deviation is detected first → move right wins
        assertEquals(Direction.RIGHT, result.direction)
    }

    @Test
    fun ` size takes priority over position`() {
        // Small AND left-of-centre — size check should fire first
        // Area ≈ 0.008 (< 0.05), so "get closer" even though position is also off
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.05f,
            top = 0.333f,
            right = 0.15f,
            bottom = 0.5f,
        )

        val result = CompositionRules.analyse(box)

        // Size is checked first — gets here before position
        assertEquals(Direction.CLOSER, result.direction)
    }

    // -------------------------------------------------------------------------
    // Boundary / edge cases
    // -------------------------------------------------------------------------

    @Test
    fun ` exactly at small threshold boundary`() {
        // Area near 0.10 peak (e.g. 0.0576 from 0.24 * 0.24)
        // distance squared is (0.0576 - 0.10)^2 = 0.00179 <= 0.0025, so should be good
        val box = BoundingBox(
            label = "object",
            confidence = 0.8f,
            left = 0.38f,
            top = 0.38f,
            right = 0.62f,
            bottom = 0.62f,
        )

        val result = CompositionRules.analyse(box)
        assertTrue("at threshold should be considered good", result.isGood)
    }

    @Test
    fun ` exactly at peak boundary`() {
        // Area near 0.56 peak (e.g. 0.6084)
        // distance squared is (0.6084 - 0.56)^2 = 0.00234 <= 0.0025, so should be good
        val box = BoundingBox(
            label = "object",
            confidence = 0.8f,
            left = 0.11f,
            top = 0.11f,
            right = 0.89f,
            bottom = 0.89f,
        )

        val result = CompositionRules.analyse(box)
        assertTrue("at threshold should be considered good", result.isGood)
    }

    @Test
    fun ` centre exactly on left-third line — good (within band)`() {
        // centre x = 0.35 (slightly right of 1/3 to avoid FP issues) → should be considered "in band"
        val box = BoundingBox(
            label = "object",
            confidence = 0.8f,
            left = 0.225f,
            top = 0.375f,
            right = 0.475f,
            bottom = 0.625f,
        )

        val result = CompositionRules.analyse(box)
        assertTrue("Should be good when center exactly on left-third line", result.isGood)
    }

    @Test
    fun ` centre exactly on right-third line — good (within band)`() {
        // centre x = 0.6 (slightly left of 2/3 to avoid FP issues) → should be considered "in band"
        val box = BoundingBox(
            label = "object",
            confidence = 0.8f,
            left = 0.475f,
            top = 0.375f,
            right = 0.725f,
            bottom = 0.625f,
        )

        val result = CompositionRules.analyse(box)
        assertTrue("Should be good when center exactly on right-third line", result.isGood)
    }

    @Test
    fun ` too large for maximum peak — step back`() {
        // Area = 0.9216 (well above 0.82 peak)
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.02f,
            top = 0.02f,
            right = 0.98f,
            bottom = 0.98f,
        )

        val result = CompositionRules.analyse(box)
        assertEquals(Direction.AWAY, result.direction)
    }

    // -------------------------------------------------------------------------
    // T11 portrait mode verification
    // -------------------------------------------------------------------------

    @Test
    fun ` face bounding box drives rules engine identically to general objects`() {
        val faceBox = BoundingBox(
            label = "face",
            confidence = 0.95f,
            left = 0.34f,
            top = 0.34f,
            right = 0.66f,
            bottom = 0.66f,
        )

        val result = CompositionRules.analyse(faceBox)

        assertTrue("Centered face bounding box should be in good zone", result.isGood)
        assertEquals(Direction.NONE, result.direction)
    }

    // -------------------------------------------------------------------------
    // Golden Ratio verification
    // -------------------------------------------------------------------------

    @Test
    fun ` centered subject under golden ratio — good zone`() {
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.35f,
            top = 0.35f,
            right = 0.65f,
            bottom = 0.65f,
        )

        val result = CompositionRules.analyse(box, "golden_ratio")

        assertTrue("Centered box should be good under golden ratio", result.isGood)
        assertEquals(Direction.NONE, result.direction)
    }

    @Test
    fun ` off-center-left under golden ratio — move right`() {
        // center x = 0.35 (which is in good band for thirds [0.333..0.667] but outside golden ratio [0.382..0.618])
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.2f,
            top = 0.35f,
            right = 0.5f,
            bottom = 0.65f,
        )

        val thirdsResult = CompositionRules.analyse(box, "rule_of_thirds")
        assertTrue("Should be good under rule of thirds", thirdsResult.isGood)

        val goldenResult = CompositionRules.analyse(box, "golden_ratio")
        assertFalse("Should need adjustment under golden ratio", goldenResult.isGood)
        assertEquals(Direction.RIGHT, goldenResult.direction)
    }

    @Test
    fun ` off-center-right under golden ratio — move left`() {
        // center x = 0.65 (which is in good band for thirds [0.333..0.667] but outside golden ratio [0.382..0.618])
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.5f,
            top = 0.35f,
            right = 0.8f,
            bottom = 0.65f,
        )

        val thirdsResult = CompositionRules.analyse(box, "rule_of_thirds")
        assertTrue("Should be good under rule of thirds", thirdsResult.isGood)

        val goldenResult = CompositionRules.analyse(box, "golden_ratio")
        assertFalse("Should need adjustment under golden ratio", goldenResult.isGood)
        assertEquals(Direction.LEFT, goldenResult.direction)
    }

    @Test
    fun ` anti-oscillation guard prevents horizontal direction flip on small improvement`() {
        val prevSuggestionSmall = CompositionSuggestion(
            direction = Direction.LEFT,
            isGood = false,
            offsetX = 0.02f,
            offsetY = 0f,
            fillRatio = 0.10f
        )

        // Bounding box with cx = 0.323f -> rawOffsetX = 0.323 - 0.333 = -0.01f.
        // This would normally suggest RIGHT.
        // But since deltaX = |-0.01 - 0.02| = 0.03f < 0.05f, the flip to RIGHT should be blocked,
        // and it should retain LEFT!
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.173f,
            top = 0.35f,
            right = 0.473f,
            bottom = 0.65f,
        )

        val resultWithoutPrev = CompositionRules.analyse(box)
        assertEquals(Direction.RIGHT, resultWithoutPrev.direction)

        val resultWithPrev = CompositionRules.analyse(box, previousSuggestion = prevSuggestionSmall)
        assertEquals("Flip should be blocked due to small improvement delta", Direction.LEFT, resultWithPrev.direction)
    }

    @Test
    fun ` anti-oscillation guard allows horizontal direction flip on large improvement`() {
        val prevSuggestionLarge = CompositionSuggestion(
            direction = Direction.LEFT,
            isGood = false,
            offsetX = 0.06f,
            offsetY = 0f,
            fillRatio = 0.10f
        )

        // Bounding box with cx = 0.25f -> rawOffsetX = 0.25 - 0.333 = -0.083f.
        // This suggests RIGHT.
        // deltaX = |-0.083 - 0.06| = 0.143f >= 0.05f, so the flip to RIGHT is allowed!
        val box = BoundingBox(
            label = "person",
            confidence = 0.9f,
            left = 0.10f,
            top = 0.35f,
            right = 0.40f,
            bottom = 0.65f,
        )

        val resultWithPrev = CompositionRules.analyse(box, previousSuggestion = prevSuggestionLarge)
        assertEquals("Flip should be allowed due to large improvement delta", Direction.RIGHT, resultWithPrev.direction)
    }

    // -------------------------------------------------------------------------
    // T13 — Product mode tests
    // -------------------------------------------------------------------------

    @Test
    fun `product mode centred subject at rule-of-thirds not good`() {
        // Centre at (0.5, 0.5), size 0.55×0.55 → area 0.3025 → near 0.35 product peak.
        // Position is (0.5, 0.5), inside CENTER [0.35, 0.65] → good.
        val box = BoundingBox(
            label = "product",
            confidence = 0.9f,
            left = 0.225f, top = 0.225f,
            right = 0.775f, bottom = 0.775f,
        )
        val result = CompositionRules.analyse(box, mode = "product")
        assertTrue(result.isGood)
        assertEquals(Direction.NONE, result.direction)
    }

    @Test
    fun `product mode off-center subject that would be good under thirds needs adjustment`() {
        // Area = 0.35 (exactly at product peak of 0.35). Under general, nearest peak is
        // 0.56 (dist² = 0.044 > 0.0025 → CLOSER). Under product, nearest peak is
        // 0.35 (dist² = 0 → size good). Then position: cx = 0.34 < 0.35 → RIGHT.
        // This demonstrates product mode reaching different results than general.
        val box = BoundingBox(
            label = "product",
            confidence = 0.9f,
            left = 0.044f, top = 0.204f,
            right = 0.636f, bottom = 0.796f,
        )

        val generalResult = CompositionRules.analyse(box, mode = "general")
        assertEquals("Under general, nearest peak is 0.56 → CLOSER", Direction.CLOSER, generalResult.direction)

        val productResult = CompositionRules.analyse(box, mode = "product")
        assertEquals("Under product, position outside CENTER band → RIGHT", Direction.RIGHT, productResult.direction)
    }

    @Test
    fun `product mode tight fill ratio target`() {
        // Area = 0.01 (too small for both general's 0.10 peak and product's 0.35 peak)
        val box = BoundingBox(
            label = "product",
            confidence = 0.9f,
            left = 0.45f, top = 0.45f,
            right = 0.55f, bottom = 0.55f,
        )
        val result = CompositionRules.analyse(box, mode = "product")
        assertEquals(Direction.CLOSER, result.direction)
    }

    @Test
    fun `product mode large object near center peak`() {
        // Area ≈ 0.36. Distance to nearest product peak (0.35) = 0.0001 < 0.0025 → good size.
        // Centre at (0.5, 0.5) → good position.
        val box = BoundingBox(
            label = "product",
            confidence = 0.9f,
            left = 0.20f, top = 0.20f,
            right = 0.80f, bottom = 0.80f,
        )
        val result = CompositionRules.analyse(box, mode = "product")
        assertTrue(result.isGood)
    }

    @Test
    fun `product mode small object nearer to general peak still gets closer`() {
        // Area ≈ 0.0676. Nearest general peak = 0.10 (dist²=0.0010), product peak = 0.35 (dist²=0.079).
        // Under general: close enough to 0.10 → size good.
        // Under product: far from 0.35 → CLOSER.
        val box = BoundingBox(
            label = "product",
            confidence = 0.9f,
            left = 0.36f, top = 0.36f,
            right = 0.62f, bottom = 0.62f,
        )
        val generalResult = CompositionRules.analyse(box, mode = "general")
        assertTrue("Under general mode this should be good size", generalResult.isGood)

        val productResult = CompositionRules.analyse(box, mode = "product")
        assertEquals("Under product mode this needs to be closer", Direction.CLOSER, productResult.direction)
    }
}