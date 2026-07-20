package com.framecoach.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framecoach.app.ui.theme.MochaBase
import com.framecoach.app.ui.theme.MochaGreen
import com.framecoach.app.ui.theme.MochaMauve
import com.framecoach.app.ui.theme.MochaSubtext0
import com.framecoach.app.ui.theme.MochaText

/**
 * First-launch onboarding overlay (B1).
 *
 * Shown once — on the very first app open — to explain the three key UI elements:
 * grid lines, directional arrows, and the haptic pulse.
 *
 * Visibility is controlled by the caller via [visible]; the overlay fades in/out
 * so it doesn't feel abrupt against the live camera preview.
 *
 * @param visible  Whether to display the overlay.
 * @param onDismiss  Called when the user taps "Got it"; the caller should persist
 *                   [AppPreferences.setOnboardingShown] and set [visible] to false.
 */
@Composable
fun OnboardingOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // Semi-transparent scrim over the entire camera preview.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MochaBase.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ---- Title ----
                Text(
                    text = "How FrameCoach works",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MochaText,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Real-time composition guidance, 100% on device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MochaSubtext0,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ---- Three tips ----
                OnboardingTip(
                    icon = Icons.Default.GridOn,
                    title = "Grid lines",
                    body = "The rule-of-thirds grid helps you place subjects on power points.",
                )

                Spacer(modifier = Modifier.height(20.dp))

                OnboardingTip(
                    icon = Icons.Default.SwapVert,
                    title = "Directional arrows",
                    body = "Arrows show which way to move the camera for a stronger composition.",
                )

                Spacer(modifier = Modifier.height(20.dp))

                OnboardingTip(
                    icon = Icons.Default.CropFree,
                    title = "Good zone pulse",
                    body = "The border glows green and the phone vibrates when the framing is ideal.",
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ---- Dismiss button ----
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MochaMauve,
                        contentColor = MochaBase,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = "Got it — dismiss coaching tips" },
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Got it",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

/**
 * A single tip row: icon chip on the left, title + body text on the right.
 */
@Composable
private fun OnboardingTip(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Icon chip
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = MochaMauve.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MochaMauve,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MochaText,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
                lineHeight = 18.sp,
            )
        }
    }
}
