package com.framecoach.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Catppuccin Mocha palette.
 *
 * Source: 04_Frontend_Specification_Document.md §1.
 * These are the raw constants; the actual Material 3 color scheme is assembled in [Theme.kt].
 */
// -- Foundation --
val MochaBase = Color(0xFF1E1E2E)
val MochaMantle = Color(0xFF181825)

// -- Text --
val MochaText = Color(0xFFCDD6F4)
val MochaSubtext0 = Color(0xFFA6ADC8)

// -- Semantic roles --
val MochaMauve = Color(0xFFCBA6F7)       // Accent, shutter button
val MochaGreen = Color(0xFFA6E3A1)        // Good-zone feedback
val MochaPeach = Color(0xFFFAB387)        // Needs-adjustment indicator
val MochaYellow = Color(0xFFF9E2AF)       // Warning (low light/confidence)
val MochaRed = Color(0xFFF38BA8)          // Error

// -- Surfaces / chrome --
val MochaSurface = Color(0xFF313244)      // Surface0 — card backgrounds, slightly above Mantle
val MochaOverlay1 = Color(0xFF7F849C)     // Grid lines at ~40% opacity
