package com.framecoach.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framecoach.app.ui.theme.CanvasDark
import com.framecoach.app.ui.theme.SurfaceDeep
import com.framecoach.app.ui.theme.AccentSage
import com.framecoach.app.ui.theme.TextSecondary
import com.framecoach.app.ui.theme.SurfaceMedium
import com.framecoach.app.ui.theme.TextPrimary

/**
 * Settings screen for toggling grid overlay and haptic feedback (T10).
 *
 * Designed per 04_Frontend_Specification_Document.md §3.
 * Changes are written immediately to [AppPreferences] on every toggle flip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridEnabled by prefs.gridEnabled.collectAsState()
    val hapticsEnabled by prefs.hapticsEnabled.collectAsState()
    val compositionStyle by prefs.compositionStyle.collectAsState()
    val audioCoachingEnabled by prefs.audioCoachingEnabled.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to camera",
                            tint = TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDeep,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // --- Visual section ---
            SectionLabel(text = "Viewfinder")

            Spacer(modifier = Modifier.height(8.dp))

            SectionCard {
                SettingsToggleRow(
                    icon = Icons.Default.GridOn,
                    title = "Grid overlay",
                    subtitle = "Show composition guide lines over the viewfinder",
                    checked = gridEnabled,
                    onCheckedChange = { prefs.setGridEnabled(it) },
                )

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Haptic feedback",
                    subtitle = "Vibrate once when the composition enters the good zone",
                    checked = hapticsEnabled,
                    onCheckedChange = { prefs.setHapticsEnabled(it) },
                )

                SettingsDivider()

                SettingsToggleRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Audio coaching",
                    subtitle = "Speak directional cues aloud using on-device speech",
                    checked = audioCoachingEnabled,
                    onCheckedChange = { prefs.setAudioCoachingEnabled(it) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Composition section ---
            SectionLabel(text = "Composition")

            Spacer(modifier = Modifier.height(8.dp))

            SectionCard {
                SettingsStyleRow(
                    icon = Icons.Default.Style,
                    title = "Grid style",
                    subtitle = "Rule of thirds or golden ratio (phi grid)",
                    selectedStyle = compositionStyle,
                    onStyleChange = { prefs.setCompositionStyle(it) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared section components
// ---------------------------------------------------------------------------

/**
 * Section header label rendered above each card group.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AccentSage,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier.padding(start = 4.dp),
    )
}

/**
 * Wraps child items in a rounded card over MochaSurface.
 */
@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceMedium,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        content()
    }
}

/**
 * Thin horizontal divider used between rows inside a SectionCard.
 */
@Composable
private fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 52.dp)
            .background(SurfaceMedium.copy(alpha = 0.15f)),
    )
}

// ---------------------------------------------------------------------------
// Private components
// ---------------------------------------------------------------------------

/**
 * A single settings row: leading icon, title + subtitle text, trailing switch.
 */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentSage,
            modifier = Modifier
                .background(
                    color = AccentSage.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(8.dp),
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CanvasDark,
                checkedTrackColor = AccentSage,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceDeep,
            ),
        )
    }
}

/**
 * A settings row for choosing the composition grid style.
 */
@Composable
private fun SettingsStyleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentSage,
                modifier = Modifier
                    .background(
                        color = AccentSage.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(8.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SurfaceDeep,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isThirds = selectedStyle == AppPreferences.STYLE_RULE_OF_THIRDS
            val isGolden = selectedStyle == AppPreferences.STYLE_GOLDEN_RATIO

            Text(
                text = "Rule of Thirds",
                color = if (isThirds) CanvasDark else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isThirds) AccentSage else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onStyleChange(AppPreferences.STYLE_RULE_OF_THIRDS) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = "Golden Ratio",
                color = if (isGolden) CanvasDark else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isGolden) AccentSage else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onStyleChange(AppPreferences.STYLE_GOLDEN_RATIO) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
