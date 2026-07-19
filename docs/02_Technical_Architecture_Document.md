# Technical Architecture Document
## AI Camera Composition Assistant

---

### 1. Tech Stack

| Layer | Choice | Reasoning |
|---|---|---|
| Language | Kotlin | Standard for modern Android; you're on Linux so Android Studio + Kotlin needs no Mac, unlike iOS/Swift |
| Min SDK | API 24 (Android 7.0) | Wide compatibility with budget/older devices — your actual target market |
| Camera access | CameraX (`ImageAnalysis` use case) | Handles device fragmentation for you; gives a clean per-frame callback to run detection on |
| On-device detection | MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`) | Google's supported on-device inference library; ships with pre-trained, mobile-optimized models — no need to train your own detector for v1 |
| Detection model | EfficientDet-Lite0 (object detection, COCO-trained) for general subjects; MediaPipe Face Detector task for portrait mode | Lite0 is the recommended balance of latency vs. accuracy for real-time mobile use; swap to Face Detector when the "portrait" mode is selected, since it's cheaper and more accurate for that specific case |
| UI | Jetpack Compose (`Canvas` for the grid/overlay, layered over CameraX's `PreviewView`) | Modern, less boilerplate than XML layouts; Canvas gives full control for drawing dynamic overlays |
| Concurrency | Kotlin Coroutines | For running detection off the main thread without blocking the camera preview |
| Local storage (if/when needed) | Room (SQLite wrapper) | Only needed if you add a shot history/settings persistence; not required for a bare MVP |
| Backend | **None for v1** | Everything runs on-device; no server, no API keys, no accounts |

### 2. File & Folder Structure

```
app/
  src/main/
    java/com/yourpackage/framecoach/
      camera/          # CameraX setup, PreviewView binding, ImageAnalysis pipeline
      detection/        # MediaPipe wrapper — loads model, runs inference, returns bounding boxes
      rules/             # Composition rules engine — rule of thirds, fill-ratio, horizon check
                          # (this folder is your actual IP — no library does this part for you)
      ui/
        overlay/         # Compose Canvas grid + directional indicator
        capture/          # Shutter button, capture flow, save-to-gallery
        settings/         # Toggle grid/haptics screens
      sensors/           # Accelerometer wrapper for horizon leveling
      di/                # Dependency injection wiring (Hilt, optional but recommended once >1 screen)
    assets/
      models/
        efficientdet_lite0.tflite
    AndroidManifest.xml
```

### 3. Database Schema

Not required for the MVP — the app has no accounts and no server, so there's nothing to persist beyond what Android's own gallery/media store already handles for saved photos.

If you later add a shot history or settings screen backed by local storage:

**Shots table** (Room, local only)
| Field | Type | Notes |
|---|---|---|
| id | Long (PK, autoincrement) | |
| timestamp | Long | Epoch millis |
| thumbnailPath | String | Local file path |
| compositionScore | Float (nullable) | Only populated once/if the v2 learned-scoring model exists |
| mode | String | e.g. "portrait", "product", "general" |

**Settings table**
| Field | Type | Notes |
|---|---|---|
| gridEnabled | Boolean | |
| hapticsEnabled | Boolean | |
| preferredMode | String | Last-used mode |

No multi-user schema needed — this is a single-user, single-device app by design.

### 4. Environment & Config Notes

- **No API keys required** for v1 — there is no third-party service being called. This is a deliberate architectural choice, not an oversight: it removes an entire class of security concerns (leaked keys, quota costs, server downtime) that cloud-based competitors like Camera Coach have to deal with.
- The `.tflite` model file ships bundled inside the APK's `assets/` folder — no download step, works from first launch, fully offline.
- Add ProGuard/R8 keep rules for the MediaPipe classes so release builds don't strip required reflection-based code:
  ```
  -keep class com.google.mediapipe.** { *; }
  ```
- Required permissions in `AndroidManifest.xml`:
  - `CAMERA` — for the preview/analysis pipeline
  - `VIBRATE` — for haptic feedback
  - Scoped storage handles saving to gallery on API 29+; no broad storage permission needed on modern targets
- If you add cloud sync or an aesthetic-scoring API in v2, that's the point where you'd introduce environment variables and a `local.properties`-based key management setup — deliberately deferred until it's actually needed.
