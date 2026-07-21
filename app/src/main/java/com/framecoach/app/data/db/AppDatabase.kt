package com.framecoach.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room database for FrameCoach offline data (C2).
 *
 * 100% offline, single-user, on-device storage.
 */
@Database(
    entities = [ShotRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shotRecordDao(): ShotRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "framecoach_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
