package com.framecoach.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.framecoach.app.detection.ExposureState
import com.framecoach.app.ui.overlay.CompositionOverlay
import com.framecoach.app.ui.overlay.CompositionState
import com.framecoach.app.ui.overlay.HapticController
import com.framecoach.app.ui.overlay.HorizonOverlay
import com.framecoach.app.ui.settings.AppPreferences
import com.framecoach.app.sensors.HorizonSensor
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import com.framecoach.app.ui.theme.MochaBase
import com.framecoach.app.ui.theme.MochaMantle
import com.framecoach.app.ui.theme.MochaMauve
import com.framecoach.app.ui.theme.MochaSubtext0
import com.framecoach.app.ui.theme.MochaText
import com.framecoach.app.ui.theme.MochaYellow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Top-level camera screen: manages the CAMERA runtime-permission lifecycle.
 *
 * Renders [CameraScreen] when permission is granted, or [PermissionDeniedScreen] when
 * the user denies. Distinguishes between "can re-ask" and "permanently denied" states
 * per 03_Security_Access_Document.md section 4.
 *
 * Also includes a shutter button to capture and save full-resolution photos to the gallery,
 * and a settings gear icon to navigate to the settings screen (T10).
 *
 * @param prefs              App preferences (grid + haptics toggles) from [AppPreferences].
 * @param onNavigateToSettings  Callback to open the settings screen.
 */
@Composable
fun CameraScreen(
    prefs: AppPreferences,
    onNavigateToSettings: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // T10: collect persisted user preferences.
    val gridEnabled by prefs.gridEnabled.collectAsState()
    val hapticsEnabled by prefs.hapticsEnabled.collectAsState()
    val cameraMode by prefs.cameraMode.collectAsState()
    val compositionStyle by prefs.compositionStyle.collectAsState()

    // T12: collect exposure state
    val exposureResult by ExposureState.result.collectAsState()

    var permissionState by androidx.compose.runtime.remember { mutableStateOf<CameraPermissionState>(CameraPermissionState.Unknown) }
    var hasRequestedBefore by androidx.compose.runtime.remember { mutableStateOf(false) }
    var imageCapture by androidx.compose.runtime.remember { mutableStateOf<ImageCapture?>(null) }

    // Zoom & flash state
    var camera by remember { mutableStateOf<Camera?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var showZoomLevel by remember { mutableStateOf(false) }
    var hideZoomJob by remember { mutableStateOf<Job?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    val scope = rememberCoroutineScope()

    // T8: haptic controller — one instance per composition, reset when context changes.
    val hapticController = remember(context) { HapticController(context) }

    // T8: collect composition results and forward to the haptic controller.
    // Gate on the hapticsEnabled preference so the user can silence vibrations.
    LaunchedEffect(hapticsEnabled) {
        CompositionState.suggestion.collect { suggestion ->
            if (hapticsEnabled) {
                hapticController.onCompositionUpdate(suggestion.isGood)
            }
        }
    }

    // T9: horizon sensor — one instance per composition, tied to the lifecycle owner.
    val horizonSensor = remember(context) { HorizonSensor(context) }
    DisposableEffect(lifecycleOwner) {
        horizonSensor.start()
        onDispose { horizonSensor.stop() }
    }

    val requestPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionState = CameraPermissionState.Granted
        } else {
            permissionState = if (
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, Manifest.permission.CAMERA
                )
            ) {
                CameraPermissionState.Denied
            } else if (hasRequestedBefore) {
                CameraPermissionState.PermanentlyDenied
            } else {
                CameraPermissionState.Denied
            }
            hasRequestedBefore = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionState = CameraPermissionState.Granted
            }
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity, Manifest.permission.CAMERA
            ) -> {
                permissionState = CameraPermissionState.Denied
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Function to take a picture and save it to the gallery
    fun takePicture(capture: androidx.camera.core.ImageCapture?) {
        if (capture == null) return
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timestamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DCIM + "/Camera"
                )
            }
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val outputFileOptions = OutputFileOptions.Builder(resolver, collection, contentValues).build()
        capture.takePicture(
            outputFileOptions,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                override fun onError(exc: androidx.camera.core.ImageCaptureException) {
                    Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                    val msg = "Photo saved: ${output.savedUri}"
                    Log.d("CameraScreen", msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    when (permissionState) {
        CameraPermissionState.Granted -> {
            // Use a Box as the root to allow stacking.
            Box(modifier = modifier.fillMaxSize()) {
                // This box contains the camera preview and overlay, filling the entire screen.
                // Pinch-to-zoom gesture is detected at this level.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val cam = camera ?: return@detectTransformGestures
                                val zs = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                                val newZoom = (currentZoomRatio * zoom)
                                    .coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                                cam.cameraControl.setZoomRatio(newZoom)
                                currentZoomRatio = newZoom
                                showZoomLevel = true
                                hideZoomJob?.cancel()
                                hideZoomJob = scope.launch {
                                    delay(2000L)
                                    showZoomLevel = false
                                }
                            }
                        }
                ) {
                    CameraPreview(
                        lifecycleOwner = lifecycleOwner,
                        cameraMode = cameraMode,
                        compositionStyle = compositionStyle,
                        modifier = Modifier.fillMaxSize(),
                        onImageCaptureChanged = { ic -> imageCapture = ic },
                        onCameraReady = { cam -> camera = cam }
                    )
                    // T10: pass showGrid preference to the overlay.
                    CompositionOverlay(
                        showGrid = gridEnabled,
                        compositionStyle = compositionStyle
                    )
                    HorizonOverlay(horizonSensor = horizonSensor)

                    // Zoom level indicator — overlay below the flash toggle at top-left
                    if (showZoomLevel) {
                        Text(
                            text = "${"%.1f".format(currentZoomRatio)}x",
                            color = MochaText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 56.dp)
                                .background(
                                    color = MochaBase.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    // T12: Exposure warning badge — below the zoom indicator
                    if (exposureResult.isWarning) {
                        val label = when {
                            exposureResult.isUnderexposed -> "UNDEREXPOSED"
                            else -> "OVEREXPOSED"
                        }
                        val badgeColor = MochaYellow
                        Text(
                            text = label,
                            color = MochaBase,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 88.dp)
                                .background(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Flash toggle — top-left corner.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    val flashIcon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    val flashDesc = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> "Flash on"
                        ImageCapture.FLASH_MODE_AUTO -> "Flash auto"
                        else -> "Flash off"
                    }
                    IconButton(
                        onClick = {
                            if (imageCapture == null) return@IconButton
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            imageCapture?.setFlashMode(flashMode)
                        },
                        enabled = imageCapture != null,
                    ) {
                        Icon(
                            imageVector = flashIcon,
                            contentDescription = flashDesc,
                            tint = if (imageCapture == null) MochaMantle else when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> MochaSubtext0
                                ImageCapture.FLASH_MODE_AUTO -> MochaYellow
                                else -> MochaYellow
                            },
                        )
                    }
                }

                // Zoom +/- buttons — right edge.
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(
                        onClick = {
                            val cam = camera ?: return@IconButton
                            val zs = cam.cameraInfo.zoomState.value ?: return@IconButton
                            val range = zs.maxZoomRatio - zs.minZoomRatio
                            val zoomStep = (range / 10f).coerceAtLeast(0.1f)
                            val newZoom = (currentZoomRatio + zoomStep)
                                .coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                            cam.cameraControl.setZoomRatio(newZoom)
                            currentZoomRatio = newZoom
                            showZoomLevel = true
                            hideZoomJob?.cancel()
                            hideZoomJob = scope.launch {
                                delay(2000L)
                                showZoomLevel = false
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MochaBase.copy(alpha = 0.7f),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom in",
                            tint = MochaText,
                        )
                    }
                    IconButton(
                        onClick = {
                            val cam = camera ?: return@IconButton
                            val zs = cam.cameraInfo.zoomState.value ?: return@IconButton
                            val range = zs.maxZoomRatio - zs.minZoomRatio
                            val zoomStep = (range / 10f).coerceAtLeast(0.1f)
                            val newZoom = (currentZoomRatio - zoomStep)
                                .coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                            cam.cameraControl.setZoomRatio(newZoom)
                            currentZoomRatio = newZoom
                            showZoomLevel = true
                            hideZoomJob?.cancel()
                            hideZoomJob = scope.launch {
                                delay(2000L)
                                showZoomLevel = false
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MochaBase.copy(alpha = 0.7f),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            tint = MochaText,
                        )
                    }
                }

                // Settings gear icon — top-right corner.
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MochaSubtext0.copy(alpha = 0.85f),
                    )
                }
                // Shutter button & camera mode selector at the bottom center.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // T11: Camera mode selector row
                    Row(
                        modifier = Modifier
                            .background(
                                color = MochaMantle.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERAL",
                            color = if (cameraMode == AppPreferences.MODE_GENERAL) MochaMauve else MochaSubtext0,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { prefs.setCameraMode(AppPreferences.MODE_GENERAL) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "PRODUCT",
                            color = if (cameraMode == AppPreferences.MODE_PRODUCT) MochaMauve else MochaSubtext0,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { prefs.setCameraMode(AppPreferences.MODE_PRODUCT) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "PORTRAIT",
                            color = if (cameraMode == AppPreferences.MODE_PORTRAIT) MochaMauve else MochaSubtext0,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { prefs.setCameraMode(AppPreferences.MODE_PORTRAIT) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            if (imageCapture != null) takePicture(imageCapture) 
                        },
                        enabled = imageCapture != null,
                        modifier = Modifier
                            .size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MochaMauve,
                            contentColor = MochaBase,
                            disabledContainerColor = MochaMantle
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture photo",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
        CameraPermissionState.Denied -> {
            PermissionDeniedScreen(
                isPermanent = false,
                onRequestPermission = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = modifier
            )
        }
        CameraPermissionState.PermanentlyDenied -> {
            PermissionDeniedScreen(
                isPermanent = true,
                onRequestPermission = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = modifier
            )
        }
        CameraPermissionState.Unknown -> {
            Box(modifier = modifier.fillMaxSize())
        }
    }
}