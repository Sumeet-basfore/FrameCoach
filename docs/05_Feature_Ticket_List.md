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
- *Description:* Port the logic validated in T2 into the `rules/` package — takes a bounding box + frame dimensions, returns a directional suggestion and a "good zone" boolean. Fill-ratio scoring should target the nearest of three statistical peaks (~10%, 56%, 82% of frame area — corresponding to wide/medium/close shots) via a quadratic cost function, rather than a single arbitrary "ideal size."
- *Acceptance criteria:* Unit tests cover at least: centered subject, off-center-left subject, subject near each of the three fill-ratio peaks, and a subject between two peaks (should resolve to the nearest one).
- *Dependencies:* T2, T3
- *Priority:* Must-have

**T4B — Anti-oscillation guard for directional guidance**
- *Description:* Add a minimum-improvement threshold before the rules engine changes its directional suggestion, and a sanity bound on how large a repositioning it will ever suggest. Addresses a documented Camera Coach failure mode: contradictory step-by-step instructions and physically unreasonable demands.
- *Acceptance criteria:* Given a subject hovering near a decision boundary (e.g. right at the edge between two grid thirds), the suggested direction does not flip on every frame — it requires a sustained delta before switching. Suggested repositioning never exceeds a bounded magnitude.
- *Dependencies:* T4
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
- *Description:* Add logic to skip detection on every Nth frame, with the skip rate adjusting based on the device's actual thermal status via `PowerManager.addThermalStatusListener` (API 29+) — not inferred from frame-processing time trends alone. Step down thread count and/or target FPS as thermal status escalates.
- *Acceptance criteria:* On a Snapdragon 680-class test device, a 10-minute continuous session (the window where budget SoCs typically begin throttling) shows the app proactively reducing detection frequency before the OS forces a hard throttle.
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

**T14 — Golden ratio target points in rules engine**
- *Description:* Extend the rules engine to compute a second set of target points using golden ratio proportions (~38.2%/61.8%, vs. the existing rule-of-thirds 33%/67%) — same calculation, different constant. Add auto-selection logic: given the subject's current position, compute the required correction under both the thirds points and the golden points, and use whichever needs the smaller correction. Output which system was selected so the UI layer knows what to render.
- *Acceptance criteria:* Unit tests confirm the engine picks thirds vs. golden correctly based on which set of points is nearer to the subject's current position for a range of test positions.
- *Dependencies:* T4
- *Priority:* Must-have

**T15 — Golden ratio grid rendering**
- *Description:* Extend the existing grid-drawing Canvas code to also support golden ratio line positions — same rendering logic as the rule-of-thirds grid, fed different coordinates.
- *Acceptance criteria:* Grid lines visibly shift to golden ratio positions when the rules engine selects golden mode.
- *Dependencies:* T5, T14
- *Priority:* Must-have

**T16 — Golden spiral rendering**
- *Description:* New Canvas path drawing a logarithmic (Fibonacci) spiral, oriented so its focal point lands on whichever of the four golden ratio target points the rules engine selected. This is the one genuinely new piece of rendering work in this feature — T14 and T15 are extensions of code that already exists.
- *Acceptance criteria:* Spiral renders correctly oriented for all four possible target points and visually converges on the correct focal point in each case.
- *Dependencies:* T5, T14
- *Priority:* Must-have

**T17 — Golden ratio style setting (Grid vs. Spiral)**
- *Description:* Add a Settings toggle for which visual style to use when golden mode is active. Purely a rendering preference — doesn't affect the auto-selection logic in T14, only which of T15/T16 gets drawn.
- *Acceptance criteria:* Toggle persists across restarts (same pattern as T10); switching it changes only the visual style, not which target point was selected.
- *Dependencies:* T10, T15, T16
- *Priority:* Should-have

**T18 — Multi-subject centroid weighting & anti-jitter filter**
- *Description:* Composite weighted center of mass calculation for multi-subject scenes with 5-frame EMA smoothing buffer.
- *Acceptance criteria:* Multi-subject centroid balances spatial weight without single-frame detection drops causing arrow direction flips.
- *Priority:* Should-have

**T19 — Horizon zero-point calibration**
- *Description:* User zero-point calibration for phone mounts/tripods stored in AppPreferences.
- *Acceptance criteria:* Calibration offset persists and shifts zero-degree level threshold accurately.
- *Priority:* Should-have

**T20 — Quadrant backlight & glare warning**
- *Description:* 4-quadrant Y-plane luminance matrix analysis detecting strong backlighting and harsh direct glare.
- *Acceptance criteria:* "STRONG BACKLIGHT" and "HARSH GLARE" badges display when background vs. subject contrast exceeds thresholds.
- *Priority:* Should-have

**T21 — Stealth viewfinder auto-dimming**
- *Description:* Auto-fading secondary controls after 1.5s in the Good Zone for ultra-clean photography.
- *Acceptance criteria:* HUD controls dim to 35% opacity while subject is framing-aligned, restoring instantly on touch or position loss.
- *Priority:* Nice-to-have