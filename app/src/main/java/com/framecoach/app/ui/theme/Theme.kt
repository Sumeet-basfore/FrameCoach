package com.framecoach.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Organic HUD Material 3 color scheme.
 *
 * Dark-first, low-glare dark theme tailored for camera coaching overlays (C1/C2).
 */
private val FrameDarkColorScheme = darkColorScheme(
    primary = AccentSage,
    onPrimary = SurfaceDeep,
    primaryContainer = AccentSage.copy(alpha = 0.12f),
    onPrimaryContainer = AccentSage,

    secondary = SuccessIce,
    onSecondary = SurfaceDeep,
    secondaryContainer = SuccessIce.copy(alpha = 0.12f),
    onSecondaryContainer = SuccessIce,

    tertiary = WarningPlum,
    onTertiary = TextPrimary,
    tertiaryContainer = WarningPlum.copy(alpha = 0.12f),
    onTertiaryContainer = TextPrimary,

    background = CanvasDark,
    onBackground = TextPrimary,
    surface = SurfaceDeep,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMedium,
    onSurfaceVariant = TextSecondary,

    error = ErrorPlum,
    onError = TextPrimary,
    errorContainer = ErrorPlum.copy(alpha = 0.12f),
    onErrorContainer = ErrorPlum,

    outline = SurfaceMedium,
    outlineVariant = SurfaceMedium.copy(alpha = 0.40f),
)

/**
 * Frame app theme — wraps [MaterialTheme] with the Organic HUD colour scheme.
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
