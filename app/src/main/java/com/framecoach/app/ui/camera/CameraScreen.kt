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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.framecoach.app.ui.overlay.AudioCoach
import com.framecoach.app.ui.overlay.CompositionOverlay
import com.framecoach.app.ui.overlay.CompositionState
import com.framecoach.app.ui.overlay.HapticController
import com.framecoach.app.ui.overlay.HorizonOverlay
import com.framecoach.app.ui.overlay.OnboardingOverlay
import com.framecoach.app.ui.settings.AppPreferences
import com.framecoach.app.sensors.HorizonSensor
import com.framecoach.app.data.db.ShotHistoryRepository
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.framecoach.app.ui.theme.CanvasDark
import com.framecoach.app.ui.theme.SurfaceDeep
import com.framecoach.app.ui.theme.SurfaceMedium
import com.framecoach.app.ui.theme.AccentSage
import com.framecoach.app.ui.theme.TextSecondary
import com.framecoach.app.ui.theme.TextPrimary
import com.framecoach.app.ui.theme.WarningPlum
import com.framecoach.app.ui.theme.SuccessIce
import com.framecoach.app.ui.theme.ErrorPlum
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.framecoach.app.data.db.ShotRecord
import com.framecoach.app.sensors.HorizonLevelCalculator
import com.framecoach.app.sensors.HorizonLevelState

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
    val sensitivity by prefs.sensitivity.collectAsState()
    // B1: onboarding — show overlay until dismissed on first launch.
    val onboardingShown by prefs.onboardingShown.collectAsState()
    // C1: audio coaching preference.
    val audioCoachingEnabled by prefs.audioCoachingEnabled.collectAsState()

    // T12: collect exposure state
    val exposureResult by ExposureState.result.collectAsState()

    // In-place UI sheets and post-capture state
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var capturedPhotoScore by remember { mutableIntStateOf(0) }
    var capturedPhotoStyle by remember { mutableStateOf("") }
    var capturedPhotoHorizonLevel by remember { mutableStateOf(true) }

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

    // C1: Audio coach — on-device TTS, fires only on direction-change edges.
    val audioCoach = remember(context) { AudioCoach(context) }
    DisposableEffect(lifecycleOwner) {
        audioCoach.init()
        onDispose { audioCoach.shutdown() }
    }
    // Reset spoken-direction state whenever the toggle is flipped so the next
    // update always fires a fresh cue rather than being suppressed as a repeat.
    LaunchedEffect(audioCoachingEnabled) {
        audioCoach.reset()
        CompositionState.suggestion.collect { suggestion ->
            audioCoach.onDirectionUpdate(suggestion.direction, audioCoachingEnabled)
        }
    }

    // T9: horizon sensor — one instance per composition, tied to the lifecycle owner.
    val horizonSensor = remember(context) { HorizonSensor(context) }
    DisposableEffect(lifecycleOwner) {
        horizonSensor.start()
        onDispose { horizonSensor.stop() }
    }

    // C2: Shot history repository — local Room DB for offline capture log.
    val shotHistoryRepo = remember(context) { ShotHistoryRepository.getInstance(context) }
    val historyList by shotHistoryRepo.allShots.collectAsState(initial = emptyList())

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
                    val savedUriString = output.savedUri?.toString() ?: ""
                    val msg = "Photo saved"
                    Log.d("CameraScreen", msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                    // Calculate score and metadata at capture time
                    val currentSuggestion = CompositionState.suggestion.value
                    val currentRoll = horizonSensor.rollDeg.value
                    val isHorizonLevel = HorizonLevelCalculator.compute(currentRoll).isLevel

                    // Score calculation:
                    // base 60. good zone = +30. level horizon = +10. alignment bonus up to 10.
                    var score = 50
                    if (currentSuggestion.isGood) score += 30
                    if (isHorizonLevel) score += 10
                    val rollError = Math.abs(currentRoll)
                    val alignmentBonus = (10f - rollError).coerceIn(0f, 10f).toInt()
                    score += alignmentBonus

                    capturedPhotoScore = score.coerceIn(0, 100)
                    capturedPhotoStyle = when (compositionStyle) {
                        "golden_ratio" -> "Golden Ratio"
                        "center_grid" -> "Center Grid"
                        else -> "Rule of Thirds"
                    }
                    capturedPhotoHorizonLevel = isHorizonLevel
                    capturedPhotoUri = savedUriString

                    // C2: record shot in local Room history DB
                    scope.launch {
                        shotHistoryRepo.recordShot(
                            imageUri = savedUriString,
                            mode = cameraMode,
                            isGoodZone = currentSuggestion.isGood,
                            suggestion = currentSuggestion.direction.displayText,
                            compositionStyle = compositionStyle,
                        )
                    }
                }
            }
        )
    }

    when (permissionState) {
        CameraPermissionState.Granted -> {
            Box(modifier = modifier.fillMaxSize()) {
                // Main Camera view Box (viewfinder & overlay)
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
                        sensitivity = sensitivity,
                        modifier = Modifier.fillMaxSize(),
                        onImageCaptureChanged = { ic -> imageCapture = ic },
                        onCameraReady = { cam -> camera = cam }
                    )
                    CompositionOverlay(
                        showGrid = gridEnabled,
                        compositionStyle = compositionStyle
                    )
                    HorizonOverlay(horizonSensor = horizonSensor)

                    // Onboarding overlay
                    OnboardingOverlay(
                        visible = !onboardingShown,
                        onDismiss = { prefs.setOnboardingShown(true) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Zoom level indicator
                    if (showZoomLevel) {
                        Text(
                            text = "${"%.1f".format(currentZoomRatio)}x",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 56.dp)
                                .background(
                                    color = CanvasDark.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    // T12: Exposure warning badge — below the zoom indicator
                    if (exposureResult.isWarning) {
                        val label = when {
                            exposureResult.isUnderexposed -> "UNDEREXPOSED"
                            exposureResult.isOverexposed -> "OVEREXPOSED"
                            exposureResult.isBacklit -> "STRONG BACKLIGHT"
                            exposureResult.isGlare -> "HARSH GLARE"
                            else -> "EXPOSURE WARNING"
                        }
                        Text(
                            text = label,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 88.dp)
                                .background(
                                    color = WarningPlum,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Flash toggle
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
                            tint = if (imageCapture == null) SurfaceDeep else when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> TextSecondary
                                ImageCapture.FLASH_MODE_AUTO -> AccentSage
                                else -> AccentSage
                            },
                        )
                    }
                }

                // Zoom +/- buttons
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
                                color = CanvasDark.copy(alpha = 0.7f),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom in",
                            tint = TextPrimary,
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
                                color = CanvasDark.copy(alpha = 0.7f),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            tint = TextPrimary,
                        )
                    }
                }

                // Settings gear icon
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open settings",
                        tint = TextSecondary.copy(alpha = 0.85f),
                    )
                }

                // Camera Controls HUD Panel (Bottom Center)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(SurfaceDeep.copy(alpha = 0.90f))
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Camera mode selector row
                    Row(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .background(
                                color = SurfaceMedium.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modes = listOf(
                            AppPreferences.MODE_GENERAL to "GENERAL",
                            AppPreferences.MODE_PRODUCT to "PRODUCT",
                            AppPreferences.MODE_PORTRAIT to "PORTRAIT"
                        )
                        modes.forEach { (modeKey, modeName) ->
                            val isActive = cameraMode == modeKey
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isActive) AccentSage else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { prefs.setCameraMode(modeKey) }
                                    .semantics { contentDescription = "$modeName mode — tap to select" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                    Text(
                                        text = modeName,
                                        color = if (isActive) CanvasDark else TextSecondary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Shutter & Utility Button Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Vibration/Haptic Toggle
                            IconButton(
                                onClick = { prefs.setHapticsEnabled(!hapticsEnabled) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceMedium, shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = "Toggle vibration feedback",
                                    tint = if (hapticsEnabled) AccentSage else TextSecondary
                                )
                            }

                            // Center: Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable(enabled = imageCapture != null) {
                                        takePicture(imageCapture)
                                    }
                                    .semantics { contentDescription = "Capture photo" },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent, shape = CircleShape)
                                        .border(4.dp, if (imageCapture != null) AccentSage else SurfaceMedium, shape = CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(CanvasDark, shape = CircleShape)
                                        .border(1.dp, SurfaceMedium, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(if (imageCapture != null) AccentSage.copy(alpha = 0.15f) else Color.Transparent, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = if (imageCapture != null) AccentSage else SurfaceMedium,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Right: Grid System Toggle
                            IconButton(
                                onClick = { prefs.setGridEnabled(!gridEnabled) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceMedium, shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = "Toggle grid lines",
                                    tint = if (gridEnabled) AccentSage else TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Navigation Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceMedium.copy(alpha = 0.5f))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Settings Tab
                            Column(
                                modifier = Modifier.clickable { showSettingsSheet = true },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Open Settings sheet",
                                    tint = if (showSettingsSheet) AccentSage else TextSecondary
                                )
                                Text(
                                    text = "SETTINGS",
                                    fontSize = 10.sp,
                                    color = if (showSettingsSheet) AccentSage else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Capture Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Active Capture mode",
                                    tint = AccentSage
                                )
                                Text(
                                    text = "CAPTURE",
                                    fontSize = 10.sp,
                                    color = AccentSage,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // History Tab
                            Column(
                                modifier = Modifier.clickable { showHistorySheet = true },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Open History sheet",
                                    tint = if (showHistorySheet) AccentSage else TextSecondary
                                )
                                Text(
                                    text = "LOGS",
                                    fontSize = 10.sp,
                                    color = if (showHistorySheet) AccentSage else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                // Settings Bottom Sheet Panel
                AnimatedVisibility(
                    visible = showSettingsSheet,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SurfaceDeep,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(color = TextSecondary.copy(alpha = 0.3f), shape = CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HUD Settings",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showSettingsSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close settings", tint = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "COACH SENSITIVITY",
                            color = AccentSage,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOW",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Slider(
                                value = sensitivity,
                                onValueChange = { prefs.setSensitivity(it) },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentSage,
                                    activeTrackColor = AccentSage,
                                    inactiveTrackColor = SurfaceMedium
                                )
                            )
                            Text(
                                text = "HIGH (${"%.2f".format(sensitivity)})",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "GRID SYSTEM STYLE",
                            color = AccentSage,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceMedium, shape = RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            val styles = listOf(
                                AppPreferences.STYLE_RULE_OF_THIRDS to "Rule of Thirds",
                                AppPreferences.STYLE_GOLDEN_RATIO to "Golden Ratio",
                                AppPreferences.STYLE_CENTER_GRID to "Center Grid"
                            )
                            styles.forEach { (styleKey, styleLabel) ->
                                val isSelected = compositionStyle == styleKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) AccentSage else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { prefs.setCompositionStyle(styleKey) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = styleLabel,
                                        color = if (isSelected) CanvasDark else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Audio Coaching Feedback", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Speak live framing suggestions", color = TextSecondary, fontSize = 12.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = audioCoachingEnabled,
                                onCheckedChange = { prefs.setAudioCoachingEnabled(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = CanvasDark,
                                    checkedTrackColor = AccentSage,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceMedium
                                )
                            )
                        }
                    }
                }

                // History Bottom Sheet Panel
                AnimatedVisibility(
                    visible = showHistorySheet,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SurfaceDeep,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(color = TextSecondary.copy(alpha = 0.3f), shape = CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Capture History",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (historyList.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                shotHistoryRepo.clearAll()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear all history", tint = ErrorPlum)
                                    }
                                }
                                IconButton(onClick = { showHistorySheet = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close history", tint = TextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No captured photos yet",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(historyList) { shot ->
                                    HistoryRow(shot, context)
                                }
                            }
                        }
                    }
                }

                // Post-Capture Preview Overlay
                val capturedUri = capturedPhotoUri
                if (capturedUri != null) {
                    PostCapturePreview(
                        uriString = capturedUri,
                        score = capturedPhotoScore,
                        style = capturedPhotoStyle,
                        isLevel = capturedPhotoHorizonLevel,
                        onRetake = { capturedPhotoUri = null },
                        onKeep = { capturedPhotoUri = null },
                        context = context,
                        modifier = Modifier.fillMaxSize()
                    )
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

@Composable
fun HistoryRow(
    shot: ShotRecord,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(shot.imageUri) {
        try {
            val uri = Uri.parse(shot.imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceMedium.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceMedium.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .background(SurfaceDeep, shape = RoundedCornerShape(8.dp))
                .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Thumbnail for capture ${shot.id}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Metadata Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val modeUpper = shot.mode.uppercase()
            val styleLabel = when (shot.compositionStyle) {
                "golden_ratio" -> "Golden Ratio"
                "center_grid" -> "Center Grid"
                else -> "Rule of Thirds"
            }
            Text(
                text = "$styleLabel • $modeUpper",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (shot.isGoodZone) "Perfect alignment" else "Suggestion: ${shot.suggestion}",
                color = if (shot.isGoodZone) SuccessIce else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            val timeString = remember(shot.timestamp) {
                val diff = System.currentTimeMillis() - shot.timestamp
                when {
                    diff < 60000 -> "Just now"
                    diff < 3600000 -> "${diff / 60000}m ago"
                    else -> {
                        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(shot.timestamp))
                    }
                }
            }
            Text(
                text = timeString,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Score Badge
        val score = if (shot.isGoodZone) 85 else 60
        Box(
            modifier = Modifier
                .background(
                    color = if (shot.isGoodZone) SuccessIce.copy(alpha = 0.1f) else WarningPlum.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (shot.isGoodZone) SuccessIce else WarningPlum,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score%",
                color = if (shot.isGoodZone) SuccessIce else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PostCapturePreview(
    uriString: String,
    score: Int,
    style: String,
    isLevel: Boolean,
    onRetake: () -> Unit,
    onKeep: () -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(uriString) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
    ) {
        // Main Image Frame
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured photo preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                    .background(SurfaceDeep, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(16.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                    .background(SurfaceDeep, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading preview...", color = TextSecondary)
            }
        }

        // Floating Metadata HUD (Top Right of image frame)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 32.dp)
                .background(
                    color = SurfaceDeep.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(1.dp, SurfaceMedium.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "ANALYSIS",
                color = AccentSage,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(120.dp)
            ) {
                Text(text = "Score:", color = TextPrimary, fontSize = 12.sp)
                Text(
                    text = "$score%",
                    color = AccentSage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(120.dp)
            ) {
                Text(text = "Style:", color = TextPrimary, fontSize = 12.sp)
                Text(text = style, color = TextPrimary, fontSize = 12.sp)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(120.dp)
            ) {
                Text(text = "Horizon:", color = TextPrimary, fontSize = 12.sp)
                Text(
                    text = if (isLevel) "Level" else "Unlevel",
                    color = if (isLevel) SuccessIce else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom control section
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CanvasDark)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder/dummy gallery thumbnail or just empty space
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(SurfaceDeep, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, SurfaceMedium, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retake Button
                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .border(1.dp, TextPrimary, shape = RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = "Retake",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Keep Button
                Button(
                    onClick = onKeep,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSage,
                        contentColor = CanvasDark
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .border(1.dp, AccentSage, shape = RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = "Keep Photo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}