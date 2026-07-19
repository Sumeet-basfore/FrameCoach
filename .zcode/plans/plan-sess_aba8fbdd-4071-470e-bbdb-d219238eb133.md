## T3 — Integrate MediaPipe Object Detector (EfficientDet-Lite0)

**Acceptance criteria (from `05_Feature_Ticket_List.md`):**
> Bounding box coordinates for detected objects are logged/visualized in real time on a physical device at ≥15fps.

### Scope

Wire MediaPipe Tasks Vision `ObjectDetector` into the existing CameraX `ImageAnalysis` pipeline. Live bounding boxes will be drawn on a Compose `Canvas` overlay on top of the preview. The detected boxes are also logged to logcat for debugging.

T4 (rules engine) and T5 (grid + directional overlay) are **not** in scope — those come after. The overlay in T3 is just simple rectangles + labels for the detected objects.

### Files to create / modify

**New files:**

| File | Purpose |
|------|---------|
| `detection/ObjectDetectorHelper.kt` | Wraps MediaPipe `ObjectDetector` — load model, detect objects in an `ImageProxy` → `List<BoundingBox>` |
| `detection/BoundingBox.kt` | Data class `(label: String, confidence: Float, left: Float, top: Float, right: Float, bottom: Float)` |
| `detection/FrameProcessor.kt` | Coroutine-based frame processor that wraps `ObjectDetectorHelper` and handles frame-skip/throttling logic (basic placeholder; full T7 throttling deferred) |
| `ui/overlay/DetectionOverlay.kt` | Simple Compose `Canvas` composable that draws colored rectangles + labels for each detected object |
| `ui/overlay/DetectionState.kt` | `StateFlow<List<BoundingBox>>` holder for passing detection results from pipeline → UI thread |

**Modified files:**

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `mediapipeTasksVision` version + library entry |
| `app/build.gradle.kts` | Add `mediapipe-tasks-vision` dependency |
| `ui/camera/CameraPreview.kt` | Add `ImageAnalysis` use case alongside `Preview`; pipe frames to `FrameProcessor` |
| `ui/camera/CameraScreen.kt` | Add `DetectionOverlay` composable layered over `CameraPreview`; collect `DetectionState` |
| `app/proguard-rules.pro` | Add MediaPipe keep rules (already present from T1 scaffold) |
| `AndroidManifest.xml` | No change needed | 

### MediaPipe Tasks Vision details

- **Artifact:** `com.google.mediapipe:tasks-vision:0.202307.17` (version to be confirmed during build; compatible with minSdk 24)
- **Model URL:** `https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/efficientdet_lite0.tflite`
- **Model path in app:** `app/src/main/assets/models/efficientdet_lite0.tflite`
- **Score threshold:** 0.5 (only show detections with ≥50% confidence)
- **Max results:** 3 objects per frame (reduce noise; full list deferred to T4)

### ImageAnalysis setup

- **Resolution:** `Size(640, 480)` (good balance of speed vs accuracy for detection)
- **Backpressure strategy:** `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` (drop frames if detector is busy)
- **Target frame rate:** Output target 30fps (CameraX will deliver what it can; detector will process at its own pace)
- **Frame-to-detector flow:** `ImageProxy` → extract `ByteBuffer`/planes → convert to `MPImage` → `ObjectDetector.detect()` → extract `List<BoundingBox>` → post to `DetectionState`

### Performance considerations (≥15fps target)

- `STRATEGY_KEEP_ONLY_LATEST` ensures we never queue frames
- `FrameProcessor` runs detection on `Dispatchers.Default` (off main thread)
- Results are posted to UI via `StateFlow` collected in the Compose scope
- EfficientDet-Lite0 benchmarks at 20-30fps on mid-range devices (snapdragon 7-series), which meets our target
- T7 will add adaptive frame-skip logic, but for T3 we keep it simple (process every frame)

### Rendering

`DetectionOverlay` is a transparent `Canvas` composable laid on top of `PreviewView` using a `Box` layout. For each bounding box in `DetectionState`:
- Draw a colored rectangle (stroked, 3px width) — Mauve/Catppuccin accent color
- Draw a small label with object name and confidence score at the top-left corner

No grid lines yet (T5), no directional arrows (T5), no rules analysis (T4).

### Verification (the "done" gate)

1. ✅ `./gradlew assembleDebug` builds successfully
2. ✅ `./gradlew testDebugUnitTest` passes (no new unit tests for T3; pure unit testing of detection is deferred)
3. 🔲 Install & run on emulator: bounding boxes appear over detected objects in the preview
4. 🔲 Check logcat for bounding box coordinates printed at ≥15fps intervals
5. 🔲 Acceptance criteria: bounding boxes visible in real time on camera preview

### Out of scope (deferred to later tickets)

- T4 rules engine (composition rules, directional suggestions)
- T5 grid overlay + directional indicator
- T7 adaptive frame-rate throttling
- T8 haptic feedback
- T11 Face Detector swap for portrait mode
- Custom ML model training (v2+ only per spec)
