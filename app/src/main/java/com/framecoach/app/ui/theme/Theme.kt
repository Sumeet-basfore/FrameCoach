package com.framecoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Frame (Catppuccin Mocha) Material 3 color scheme.
 *
 * Dark-first by design: a light theme would be distracting around a camera viewfinder
 * (04_Frontend_Specification_Document.md §1). We hard-code [darkColorScheme] rather than
 * deferring to [isSystemInDarkTheme] because switching to light mode would break the
 * intended look — even on a device that defaults to light theming, Frame stays dark.
 */
private val FrameDarkColorScheme = darkColorScheme(
    primary = MochaMauve,
    onPrimary = MochaBase,
    primaryContainer = MochaMauve.copy(alpha = 0.12f),
    onPrimaryContainer = MochaMauve,

    secondary = MochaGreen,
    onSecondary = MochaBase,
    secondaryContainer = MochaGreen.copy(alpha = 0.12f),
    onSecondaryContainer = MochaGreen,

    tertiary = MochaPeach,
    onTertiary = MochaBase,
    tertiaryContainer = MochaPeach.copy(alpha = 0.12f),
    onTertiaryContainer = MochaPeach,

    background = MochaBase,
    onBackground = MochaText,
    surface = MochaMantle,
    onSurface = MochaText,
    surfaceVariant = MochaMantle,
    onSurfaceVariant = MochaSubtext0,

    error = MochaRed,
    onError = MochaBase,
    errorContainer = MochaRed.copy(alpha = 0.12f),
    onErrorContainer = MochaRed,

    outline = MochaOverlay1,
    outlineVariant = MochaOverlay1.copy(alpha = 0.40f),
)

/**
 * Frame app theme — wraps [MaterialTheme] with the Catppuccin Mocha colour scheme.
 *
 * Call this at the root of the Compose tree (in [MainActivity.setContent]).
 */
@Composable
fun FrameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrameDarkColorScheme,
        content = content,
    )
}
