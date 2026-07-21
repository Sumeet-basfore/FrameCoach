package com.framecoach.app

import android.app.Application
import android.util.Log
import java.io.File

/**
 * Application class for the Frame composition-coach app with global crash logging.
 */
class FrameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("FrameApplication", "UNCAUGHT CRASH in thread ${thread.name}", throwable)
                val crashFile = File(getExternalFilesDir(null) ?: filesDir, "crash_log.txt")
                crashFile.writeText("Crash in thread ${thread.name}:\n" + Log.getStackTraceString(throwable))
            } catch (e: Exception) {
                Log.e("FrameApplication", "Failed to write crash log to disk", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
