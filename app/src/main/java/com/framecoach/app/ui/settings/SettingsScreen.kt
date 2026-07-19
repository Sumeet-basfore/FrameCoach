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
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.framecoach.app.ui.theme.MochaBase
import com.framecoach.app.ui.theme.MochaMantle
import com.framecoach.app.ui.theme.MochaMauve
import com.framecoach.app.ui.theme.MochaSubtext0
import com.framecoach.app.ui.theme.MochaText

/**
 * Settings screen for toggling grid overlay and haptic feedback (T10).
 *
 * Designed per 04_Frontend_Specification_Document.md §3:
 *   "standard Material list items, toggle switches for grid/haptics, no
 *    unnecessary nested navigation."
 *
 * Changes are written immediately to [AppPreferences] (and thus to
 * SharedPreferences) on every toggle flip, satisfying the acceptance criterion
 * "Toggled settings persist across app restarts."
 *
 * @param prefs            The [AppPreferences] instance to read from and write to.
 * @param onNavigateBack   Called when the user taps the back arrow.
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MochaBase,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MochaText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to camera",
                            tint = MochaText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MochaMantle,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MochaMauve,
                    )
                },
                title = "Grid overlay",
                subtitle = "Show composition guide lines over the viewfinder",
                checked = gridEnabled,
                onCheckedChange = { prefs.setGridEnabled(it) },
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MochaMantle,
            )

            SettingsToggleRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MochaMauve,
                    )
                },
                title = "Haptic feedback",
                subtitle = "Vibrate once when the composition enters the good zone",
                checked = hapticsEnabled,
                onCheckedChange = { prefs.setHapticsEnabled(it) },
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MochaMantle,
            )

            SettingsStyleRow(
                title = "Grid composition style",
                subtitle = "Choose between the rule of thirds and the golden ratio (phi grid)",
                selectedStyle = compositionStyle,
                onStyleChange = { prefs.setCompositionStyle(it) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Private components
// ---------------------------------------------------------------------------

/**
 * A single settings row: leading icon, title + subtitle text, trailing switch.
 */
@Composable
private fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading icon
        icon()

        Spacer(modifier = Modifier.width(16.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MochaText,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Trailing switch
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MochaBase,
                checkedTrackColor = MochaMauve,
                uncheckedThumbColor = MochaSubtext0,
                uncheckedTrackColor = MochaMantle,
            ),
        )
    }
}

/**
 * A settings row for choosing the composition grid style.
 */
@Composable
private fun SettingsStyleRow(
    title: String,
    subtitle: String,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MochaText,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .background(
                    color = MochaMantle,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isThirds = selectedStyle == AppPreferences.STYLE_RULE_OF_THIRDS
            val isGolden = selectedStyle == AppPreferences.STYLE_GOLDEN_RATIO
            
            Text(
                text = "Rule of Thirds",
                color = if (isThirds) MochaBase else MochaSubtext0,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(
                        color = if (isThirds) MochaMauve else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onStyleChange(AppPreferences.STYLE_RULE_OF_THIRDS) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = "Golden Ratio",
                color = if (isGolden) MochaBase else MochaSubtext0,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(
                        color = if (isGolden) MochaMauve else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onStyleChange(AppPreferences.STYLE_GOLDEN_RATIO) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
