# BRAIN.md - Camera AI Composition Assistant Project

## Project Overview
Real-time, fully offline camera-composition coach for Android. Analyzes the live camera feed and tells the user how to reposition the camera for a better shot — rule of thirds, subject framing, horizon level. It does **not** generate, enhance, retouch, or edit images. Coaching only, never image manipulation.

## Tech Stack
- Kotlin, Android, min SDK 24
- CameraX (`ImageAnalysis` use case) for camera preview and frame access
- MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`) — EfficientDet-Lite0 for general object detection
- Jetpack Compose (`Canvas`) for the grid/overlay UI, layered over `PreviewView`
- Kotlin Coroutines for off-main-thread frame processing
- No backend, no server, no cloud AI calls, no login

## Current Status (as of 2026-07-20)
All must-have tickets T1–T7, should-have T8–T10, and nice-to-have T11–T13 are complete. Build compiles; **82 unit tests passing**.

### Completed Tasks
- **T1**: CameraX setup (Preview use case) - completed
- **T2**: Python prototype of composition rules - completed
- **T3**: MediaPipe Object Detector integration (bounding box visualization) - completed
  - Verified bundled model `efficientdet_lite0.tflite` is fully INT8 quantized (input/output tensors are `INT8`).
- **T4**: Ported composition rules engine from Python to Kotlin (pure functions) - completed
  - Retrofitted fill-ratio logic with a three-peak approach (`10%`, `56%`, `82%` frame area) scored via quadratic distance to nearest peak.
- **T4B (Anti-Oscillation & Cap)**: Net-new logic - completed
  - Implemented direction-change flip guards based on a minimum improvement threshold of `0.05f`.
  - Added repositioning caps (`0.30f` of frame dimension) on target horizontal/vertical suggestions.
- **T5**: Grid + directional overlay UI (`CompositionOverlay` & `CompositionState`) - completed
- **T6**: Shutter button and photo capture saving via MediaStore API - completed
- **T7**: Adaptive frame-rate throttling via `AdaptiveFrameGate` - completed
  - `AdaptiveFrameGate` (pure Kotlin): tracks EMA of detection latency, adapts skip interval between 1–8.
  - Added Android thermal status listener (`PowerManager.addThermalStatusListener`) on API 29+ to dynamically force `MAX_SKIP_INTERVAL` (8) under severe thermal load or minimum skip interval of 3 under light/moderate load.
  - `FrameProcessor` registers and unregisters listener dynamically during lifecycle transitions.
  - 16 unit tests for gate covering: counter arithmetic, EMA seeding, slow/fast adaptation, MAX/MIN clamping, spike smoothing, thermal throttling overrides, and recovery.
- **T8**: Haptic "good zone" feedback - completed
  - `GoodZoneEdgeDetector` (pure Kotlin): fires only on false→true transition of `isGood`
  - `HapticController`: wraps platform `Vibrator` API (EFFECT_CLICK on API 29+, fallbacks for 24–28)
  - `CameraScreen` collects `CompositionState.suggestion` via `LaunchedEffect` and forwards to controller
  - 10 new unit tests for edge-detector covering: no spurious pulse, rising-edge, debounce, re-entry, falling-edge, reset
- **T9**: Horizon level indicator - completed
  - `HorizonLevelCalculator` (pure Kotlin, `sensors/`): maps roll° → `HorizonLevelState` (isLevel + normalisedTilt clamped to ±45°)
  - `HorizonSensor` (`sensors/`): wraps `TYPE_ACCELEROMETER` with α=0.8 low-pass filter, exposes `StateFlow<Float>` roll
  - `HorizonOverlay` (`ui/overlay/`): Canvas draws rotated spirit-level line + migrating bubble; invisible when level
  - `CameraScreen`: sensor started/stopped via `DisposableEffect(lifecycleOwner)`; overlay stacked over `CompositionOverlay`
  - 16 new unit tests for `HorizonLevelCalculator` covering: threshold boundaries, normalised tilt, clamping, custom threshold
- **T10**: Settings screen - completed
  - `AppPreferences` (SharedPreferences wrapper): `gridEnabled` + `hapticsEnabled` as `StateFlow<Boolean>`, persists across restarts
  - `SettingsScreen` (Material3 Scaffold + toggle rows): Catppuccin Mocha themed, writes to prefs on every flip
  - `CompositionOverlay` updated: new `showGrid: Boolean` parameter gates grid drawing
  - `CameraScreen` updated: collects prefs, gates haptics, passes `showGrid`, adds gear-icon settings button (top-right)
  - `MainActivity` updated: simple `var showSettings` state flag routes between `CameraScreen` and `SettingsScreen`
  - `material-icons-extended` added to `build.gradle.kts`
- **T11**: Portrait mode (Face Detector swap) - completed
  - `AppPreferences` updated: Added `cameraMode` (`general` vs `portrait`) key, StateFlow, and setter to persist preference
  - Created `FaceDetectorHelper` in `com.framecoach.app.detection` using BlazeFace short range model (`face_detector.tflite`)
  - `FrameProcessor` updated: Dynamically loads and runs `ObjectDetectorHelper` or `FaceDetectorHelper` based on active mode
  - `CameraPreview` updated: Accepts `cameraMode` and wraps with `rememberUpdatedState` to avoid capturing stale values in analyzer callback
  - `CameraScreen` updated: Added a premium, Catppuccin Mocha themed pill-shaped mode selector row above the shutter button
  - Added unit test in `RulesTest.kt` to verify face bounding boxes drive the rules engine identically to general objects (all 54 unit tests passing)
- **T12**: Exposure histogram warning - completed
  - `ExposureAnalyzer` (pure Kotlin, `detection/`): samples Y-plane luminance on 32x32 grid, classifies as overexposed (>85%) or underexposed (<25%)
  - `ExposureState` (`detection/`): shared StateFlow following `CompositionState` pattern
  - `CameraPreview`: extracts Y plane before `ImageProxy` is closed, runs exposure analysis per frame
  - `CameraScreen`: collects exposure state and shows yellow "UNDEREXPOSED" / "OVEREXPOSED" badge at top-left
  - 10 unit tests covering boundaries, checkerboard, small/asymmetric frames
- **T13**: Product photography preset mode - completed
- **T14/T15**: Golden ratio target points & grid rendering (38.2%/61.8% vs 33%/67%) - completed
- **T16**: Golden ratio logarithmic Fibonacci spiral rendering - completed
- **T17**: Jetpack Compose instrumented UI test suite (`CameraHudUiTest`) - completed
- **T18**: Multi-subject centroid weighting & anti-jitter filter (`CompositionRules.analyseMulti`) - completed
- **T19**: Horizon zero-point calibration (`HorizonLevelCalculator` & `AppPreferences`) - completed
- **T20**: Quadrant backlight & glare warning (`ExposureAnalyzer` Y-plane luma matrix) - completed
- **T21**: Stealth viewfinder auto-dimming - completed
- **Codebase Error Audit / Cleanup**: Thread-safe MediaPipe close lock, organic HUD design system tokens, lifecycle-safe haptics - completed
- **A1**: Release prep (v1.0) with ProGuard rules, signing setup, R8 shrinking - completed
- **B1**: Onboarding overlay - completed
- **B2**: Accessibility content descriptions - completed
- **C1**: Audio coaching cues (TTS) - completed
- **C2**: Offline Shot history log (Room DB) - completed
- **D2**: GitHub Actions CI pipeline (`android_ci.yml`) - completed

### Architecture Highlights
- 100% offline, zero network permissions.
- MediaPipe EfficientDet-Lite0 & BlazeFace INT8 quantized models.
- Pure Kotlin rules engine with anti-jitter EMA and anti-oscillation bounds.
- Room offline local Shot Database.
- Jetpack Compose Canvas organic HUD with Catppuccin Mocha theme palette.

## Next Steps / Up Next
- **A2**: Physical device matrix test (budget → flagship, API 24 boundary)
- **B3**: Landscape orientation lock already applied in manifest; verify overlay Canvas math if needed
- **D1**: Compose instrumented UI tests

v2+ deferred enhancements:
- Video recording with overlays burned in
- Cloud sync for shot history
- Learned aesthetic scoring model