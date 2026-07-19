package com.framecoach.app

import android.app.Application

/**
 * Minimal Application class for the Frame composition-coach app.
 *
 * No DI or heavy initialization needed in v1 per 02_Technical_Architecture_Document.md §2;
 * the architecture doc defers Hilt to when there's more than one screen. If lifecycle-aware
 * initialization becomes necessary (e.g. pref-warming MediaPipe in T3), add it here.
 */
class FrameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // No-op in v1 — reserved for future initialization.
    }
}
