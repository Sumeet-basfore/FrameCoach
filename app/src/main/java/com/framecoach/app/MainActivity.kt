package com.framecoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.framecoach.app.ui.camera.CameraScreen
import com.framecoach.app.ui.settings.AppPreferences
import com.framecoach.app.ui.settings.SettingsScreen
import com.framecoach.app.ui.theme.FrameTheme

/**
 * Single-activity entry point (T10: now hosts two screens via simple state navigation).
 *
 * Navigation is state-based (a single [Boolean] flag) rather than NavController-based
 * because the app only has two screens and the architecture doc explicitly defers Hilt /
 * NavController to when there is "more than one screen" — one settings screen doesn't
 * justify the added complexity of a full navigation graph in v1.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameTheme {
                val context = LocalContext.current
                // One AppPreferences instance for the whole app — SharedPreferences is
                // process-scoped so there's no risk of duplicate instances here.
                val prefs = remember { AppPreferences(context) }

                // Simple two-screen navigation: false = camera, true = settings.
                var showSettings by remember { mutableStateOf(false) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSettings) {
                        SettingsScreen(
                            prefs = prefs,
                            onNavigateBack = { showSettings = false },
                        )
                    } else {
                        CameraScreen(
                            prefs = prefs,
                            onNavigateToSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
