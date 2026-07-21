package com.framecoach.app.ui.camera

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.framecoach.app.ui.theme.AccentSage

/**
 * Full-screen explanation shown when [CameraPermissionState] is [Denied] or [PermanentlyDenied].
 *
 * - Denied yet re-askable: shows a "Grant camera access" button that invokes [onRequestPermission].
 * - Permanently denied: shows an "Open app settings" button that deep-links to the system
 *   permission page for the app (03_Security_Access_Document.md §4).
 *
 * @param isPermanent  true if the user checked "Don't ask again" — only a system-settings
 *                     deep-link can re-enable the permission.
 * @param onRequestPermission Callback for the "Grant" button (re-launches the permission dialog).
 */
@Composable
fun PermissionDeniedScreen(
    isPermanent: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = "Camera access needed"
    val message = if (isPermanent) {
        "Camera permission was permanently denied. To use Frame, enable camera access in system settings."
    } else {
        "Frame needs your camera to show a live composition coach. It never uploads or shares frames — all coaching happens on-device."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isPermanent) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSage,
                ),
            ) {
                Text("Open app settings")
            }
        } else {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSage,
                ),
            ) {
                Text("Grant camera access")
            }
        }
    }
}
