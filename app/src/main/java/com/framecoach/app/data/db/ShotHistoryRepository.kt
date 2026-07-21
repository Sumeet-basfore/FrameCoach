package com.framecoach.app.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for shot history log management (C2).
 */
class ShotHistoryRepository(private val dao: ShotRecordDao) {

    companion object {
        @Volatile
        private var INSTANCE: ShotHistoryRepository? = null

        fun getInstance(context: Context): ShotHistoryRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = ShotHistoryRepository(db.shotRecordDao())
                INSTANCE = instance
                instance
            }
        }
    }

    val allShots: Flow<List<ShotRecord>> = dao.getAllShotsFlow()

    suspend fun recordShot(
        imageUri: String,
        mode: String,
        isGoodZone: Boolean,
        suggestion: String,
        compositionStyle: String = "rule_of_thirds",
    ): Long {
        val record = ShotRecord(
            imageUri = imageUri,
            mode = mode,
            isGoodZone = isGoodZone,
            suggestion = suggestion,
            compositionStyle = compositionStyle,
        )
        return dao.insertShot(record)
    }

    suspend fun getShotCount(): Int = dao.getShotCount()

    suspend fun getGoodZoneShotCount(): Int = dao.getGoodZoneShotCount()

    suspend fun deleteShot(shot: ShotRecord) = dao.deleteShot(shot)

    suspend fun clearAll() = dao.clearAll()
}
