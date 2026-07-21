package com.framecoach.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local shot history log operations (C2).
 */
@Dao
interface ShotRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: ShotRecord): Long

    @Query("SELECT * FROM shot_history ORDER BY timestamp DESC")
    fun getAllShotsFlow(): Flow<List<ShotRecord>>

    @Query("SELECT * FROM shot_history ORDER BY timestamp DESC")
    suspend fun getAllShots(): List<ShotRecord>

    @Query("SELECT * FROM shot_history WHERE id = :id LIMIT 1")
    suspend fun getShotById(id: Long): ShotRecord?

    @Query("SELECT COUNT(*) FROM shot_history")
    suspend fun getShotCount(): Int

    @Query("SELECT COUNT(*) FROM shot_history WHERE isGoodZone = 1")
    suspend fun getGoodZoneShotCount(): Int

    @Delete
    suspend fun deleteShot(shot: ShotRecord)

    @Query("DELETE FROM shot_history")
    suspend fun clearAll()
}
