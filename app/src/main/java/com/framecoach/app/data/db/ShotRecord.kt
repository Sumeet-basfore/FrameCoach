package com.framecoach.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single captured photo entry in the local shot history log (C2).
 *
 * Stored 100% offline in a local Room database.
 * No server sync, no network calls, no cloud tracking.
 *
 * @property id Auto-incrementing primary key.
 * @property timestamp Unix timestamp (milliseconds) when the photo was captured.
 * @property imageUri String representation of the saved photo's MediaStore Uri or file path.
 * @property mode Camera mode active during capture ("general", "portrait", "product").
 * @property isGoodZone True if the subject was in a "good zone" according to rules engine.
 * @property suggestion Directional suggestion text at the time of capture (e.g. "good", "move left").
 * @property compositionStyle Active composition style ("rule_of_thirds" or "golden_ratio").
 */
@Entity(tableName = "shot_history")
data class ShotRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String,
    val mode: String,
    val isGoodZone: Boolean,
    val suggestion: String,
    val compositionStyle: String = "rule_of_thirds",
)
