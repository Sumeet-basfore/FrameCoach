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

## Current Status (as of 2026-07-19)
All must-have tickets T1–T7 and should-have T8–T10 are complete, with research-informed retrofits successfully completed. Codebase error audit conducted: addressed potential race conditions in detector lifecycles, aligned overlay indicators with Catppuccin Mocha theme specifications, and updated state-lifecycle keys. Build compiles; 62 unit tests passing.

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

- **Codebase Error Audit / Cleanup**:
  - Addressed potential race conditions in MediaPipe's `ObjectDetector` and `FaceDetector` helpers during lifecycle transitions. Introduced synchronized blocks with a thread-safe `closeLock` and `@Volatile` `isClosed` flag to prevent calling `detect()` on closed detectors and to avoid concurrent resource cleanup/inference execution.
  - Refined grid overlay design to strictly follow `04_Frontend_Specification_Document.md` spacing and color roles. Replaced hardcoded material colors with Catppuccin Mocha tokens (`MochaGreen` for good zone, `MochaOverlay1` at 40% opacity for default grid, `MochaPeach` for directional adjustments, `MochaRed` for limits).
  - Standardized state lifecycle tracking for haptic controls in `CameraScreen` by keying `LaunchedEffect` with the dynamic `hapticsEnabled` state.

### Next Steps / Up Next
1. **T12 — Exposure histogram warning** (nice-to-have v2):
   - Basic luminance histogram check on the analysis frame to flag significantly over/underexposed scenes.