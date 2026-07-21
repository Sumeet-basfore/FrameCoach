package com.framecoach.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ShotRecord] data class and metadata mapping logic (C2).
 */
class ShotRecordTest {

    @Test
    fun `default ShotRecord sets timestamp and rule_of_thirds composition style`() {
        val record = ShotRecord(
            imageUri = "content://media/external/images/media/123",
            mode = "general",
            isGoodZone = true,
            suggestion = "Good composition",
        )

        assertEquals("content://media/external/images/media/123", record.imageUri)
        assertEquals("general", record.mode)
        assertTrue(record.isGoodZone)
        assertEquals("Good composition", record.suggestion)
        assertEquals("rule_of_thirds", record.compositionStyle)
        assertTrue("Timestamp should be populated", record.timestamp > 0)
    }

    @Test
    fun `ShotRecord with custom composition style and mode`() {
        val record = ShotRecord(
            id = 42,
            timestamp = 1600000000000L,
            imageUri = "file:///storage/emulated/0/DCIM/Camera/IMG_1234.jpg",
            mode = "product",
            isGoodZone = false,
            suggestion = "Move left",
            compositionStyle = "golden_ratio",
        )

        assertEquals(42L, record.id)
        assertEquals(1600000000000L, record.timestamp)
        assertEquals("product", record.mode)
        assertFalse(record.isGoodZone)
        assertEquals("Move left", record.suggestion)
        assertEquals("golden_ratio", record.compositionStyle)
    }
}
