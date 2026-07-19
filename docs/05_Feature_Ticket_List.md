# Feature Ticket List
## AI Camera Composition Assistant

Ordered roughly in build sequence. Each ticket is scoped to be usable as a standalone prompt for an AI coding tool if you want to work that way.

---

**T1 — Camera preview via CameraX**
- *Description:* Set up a basic Android project with CameraX bound to `PreviewView`, requesting and handling the `CAMERA` runtime permission.
- *Acceptance criteria:* App opens directly to a live camera preview on a physical device; permission-denied state shows an explanation screen with a link to system settings.
- *Dependencies:* None (first ticket)
- *Priority:* Must-have

**T2 — Prototype composition rules in Python**
- *Description:* Before touching the Android detection pipeline, validate the rule-of-thirds offset logic and fill-ratio calculation against static test images/webcam using OpenCV + MediaPipe's Python API.
- *Acceptance criteria:* Given a bounding box and frame dimensions, the script reliably outputs a correct directional suggestion (left/right/up/down/closer/further) for at least 10 varied test images.
- *Dependencies:* None — can be done in parallel with T1
- *Priority:* Must-have

**T3 — Integrate MediaPipe Object Detector (EfficientDet-Lite0)**
- *Description:* Add the `com.google.mediapipe:tasks-vision` dependency, bundle the EfficientDet-Lite0 model in assets, and wire it into CameraX's `ImageAnalysis` use case to get live bounding boxes.
- *Acceptance criteria:* Bounding box coordinates for detected objects are logged/visualized in real time on a physical device at ≥15fps.
- *Dependencies:* T1
- *Priority:* Must-have

**T4 — Port the composition rules engine to Kotlin**
- *Description:* Port the logic validated in T2 into the `rules/` package — takes a bounding box + frame dimensions, returns a directional suggestion and a "good zone" boolean.
- *Acceptance criteria:* Unit tests cover at least: centered subject, off-center-left subject, subject too small (needs to get closer), subject too large (needs to step back).
- *Dependencies:* T2, T3
- *Priority:* Must-have

**T5 — Grid + directional overlay UI**
- *Description:* Compose `Canvas` layered over `PreviewView` drawing the 3×3 grid and a directional indicator driven by T4's output.
- *Acceptance criteria:* Grid is visible over the live preview; indicator updates in real time as the camera moves; grid changes color when a "good zone" state is reached.
- *Dependencies:* T4
- *Priority:* Must-have

**T6 — Capture & save to gallery**
- *Description:* Shutter button that captures the current frame at full resolution (not the downsampled analysis frame) and saves it via scoped storage to the device gallery.
- *Acceptance criteria:* Captured photo appears in the system gallery app at full camera resolution, independent of the lower-res analysis pipeline.
- *Dependencies:* T1
- *Priority:* Must-have

**T7 — Frame-rate throttling / adaptive performance**
- *Description:* Add logic to skip detection on every Nth frame, with the skip rate adjusting upward if per-frame processing time trends toward the device's thermal/performance limit.
- *Acceptance criteria:* On a mid-range test device, a 5-minute continuous session shows no visible preview stutter and no thermal throttling warning.
- *Dependencies:* T3
- *Priority:* Must-have

**T8 — Haptic "good zone" feedback**
- *Description:* Trigger a short vibration via the `Vibrator` API when the composition rules engine reports a "good zone" state, debounced so it doesn't fire repeatedly while held in that zone.
- *Acceptance criteria:* Single distinct pulse on entering a good zone; no repeat pulse until the frame leaves and re-enters that state.
- *Dependencies:* T4
- *Priority:* Should-have

**T9 — Horizon level indicator**
- *Description:* Read device tilt via the accelerometer/`SensorManager` and show a level indicator when the horizon is off by more than a set threshold.
- *Acceptance criteria:* Indicator visibly responds to physically tilting the device; disappears when level within threshold.
- *Dependencies:* T1
- *Priority:* Should-have

**T10 — Settings screen**
- *Description:* Basic screen to toggle grid visibility and haptic feedback on/off, persisted locally (SharedPreferences is sufficient at this scale — Room is overkill for two booleans).
- *Acceptance criteria:* Toggled settings persist across app restarts.
- *Dependencies:* T5, T8
- *Priority:* Should-have

**T11 — Portrait mode (Face Detector swap)**
- *Description:* Add a mode toggle that swaps the object detector for MediaPipe's Face Detector task when the user selects "portrait" mode.
- *Acceptance criteria:* Face-based bounding box drives the same rules engine from T4 with no code duplication.
- *Dependencies:* T3, T4
- *Priority:* Nice-to-have (v1.1)

**T12 — Exposure histogram warning**
- *Description:* Basic luminance histogram check on the analysis frame to flag significantly over/underexposed scenes.
- *Acceptance criteria:* Warning indicator appears on a deliberately over/underexposed test scene, disappears on a well-lit one.
- *Dependencies:* T3
- *Priority:* Nice-to-have (v2)

**T13 — Product photography preset mode**
- *Description:* Alternate rule weighting tuned for flat-lay/product shots (centered subject preferred over rule-of-thirds offset, tighter fill-ratio target).
- *Acceptance criteria:* Mode selectable from the bottom sheet; rules engine behavior visibly differs from general mode on the same test scene.
- *Dependencies:* T4, T10
- *Priority:* Nice-to-have (v2)
