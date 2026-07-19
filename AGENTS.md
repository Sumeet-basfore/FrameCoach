# AGENTS.md

This is the vendor-neutral instruction file for this project, following the open [agents.md](https://agents.md) standard — readable by most AI coding tools (Codex, Cursor, Copilot, Windsurf, Aider, Gemini CLI, Devin, and others). `CLAUDE.md` and `GEMINI.md` also exist in this repo for Claude Code and Antigravity specifically; all three encode the same constraints deliberately. If any of them ever disagree, that's a sign one was edited in isolation — flag it rather than picking one to follow.

## Project overview

Real-time, fully offline camera-composition coach for Android. Analyzes the live camera feed and tells the user how to reposition the camera for a better shot — rule of thirds, subject framing, horizon level. It does **not** generate, enhance, retouch, or edit images. Coaching only, never image manipulation — don't blur this line while implementing features.

Full specs (read before working in the relevant area):
- `01_PRD_AI_Camera_Composition_Assistant.md` — problem, users, features, explicit non-goals
- `02_Technical_Architecture_Document.md` — stack, folder structure, schema
- `03_Security_Access_Document.md` — auth (none), error handling, edge cases
- `04_Frontend_Specification_Document.md` — colors, typography, component styles
- `05_Feature_Ticket_List.md` — build order; work tickets in this sequence, don't skip ahead

## Tech stack

- Kotlin, Android, min SDK 24
- CameraX (`ImageAnalysis` use case) for camera preview and frame access
- MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`) — EfficientDet-Lite0 for general object detection, Face Detector task for portrait mode
- Jetpack Compose (`Canvas`) for the grid/overlay UI, layered over `PreviewView`
- Kotlin Coroutines for off-main-thread frame processing
- No backend, no server, no cloud AI calls, no login (see Security considerations)

## Dev environment tips

```
./gradlew assembleDebug      # build a debug APK
./gradlew installDebug       # install to a connected/emulated device
```
Model asset (`efficientdet_lite0.tflite`) lives in `app/src/main/assets/models/` and is bundled into the APK — no download step needed at runtime.

## Testing instructions

```
./gradlew testDebugUnitTest
```
The `rules/` package (composition rules engine) is pure Kotlin with no Android framework dependencies — write and run unit tests for it directly, without needing a device or emulator. Any change to the rules engine should have a passing test before being considered done.

## Code style / conventions

- Keep `rules/` as pure functions: bounding box + frame dimensions in, directional suggestion + "good zone" boolean out.
- Follow the folder structure in `02_Technical_Architecture_Document.md`:
  ```
  camera/      # CameraX setup, ImageAnalysis pipeline
  detection/   # MediaPipe wrapper — model loading, inference, bounding boxes out
  rules/       # Composition rules engine — pure functions
  ui/overlay/  # Compose Canvas grid + directional indicator
  ui/capture/  # Shutter button, capture flow, save-to-gallery
  ui/settings/ # Grid/haptics toggles
  sensors/     # Accelerometer wrapper for horizon leveling
  ```
- Match colors/typography to `04_Frontend_Specification_Document.md` (Catppuccin Mocha–based palette) rather than defaulting to Material's stock colors.
- Work through `05_Feature_Ticket_List.md` in order. Don't start a "should-have" or "nice-to-have" ticket while a "must-have" ticket is incomplete.

## Security considerations

- **No network calls anywhere in the detection/rules pipeline.** Offline-first is the product's core differentiation versus cloud-based competitors — flag it instead of adding a network call if a task seems to need one.
- **No login/auth flow in v1.** Single-user, on-device, no accounts, no server-side data of any kind.
- **No image generation, enhancement, or auto-retouch.** This app advises on framing before capture; it never modifies the captured photo afterward.
- **Detection must never block the camera preview thread.** Use coroutines off the main thread; skip a frame rather than stall one.
- **No custom-trained ML models in v1.** MediaPipe's pre-trained tasks and rule-based heuristics only — a learned aesthetic-scoring model is deferred to v2+.

## Performance & behavior constraints

- **Detection model must be INT8-quantized.** Float variants measured at 300ms+/frame on Snapdragon 680-class budget hardware — unusable for real-time use. Ship INT8 only.
- **Directional guidance must never oscillate or demand unsafe/impossible repositioning.** Require a minimum-improvement threshold before flipping a suggestion, and bound the maximum repositioning ever suggested — a documented failure mode in Google's own Camera Coach.
- **Use the Android thermal status API for adaptive throttling**, not just frame-processing time trends — `PowerManager.addThermalStatusListener` (API 29+) gives the OS's actual signal.

## PR instructions

- Run `./gradlew testDebugUnitTest` before considering any task complete.
- Before starting a ticket from `05_Feature_Ticket_List.md`, restate its acceptance criteria and a one- or two-sentence implementation plan.
- Do not introduce features from the "What we are deliberately NOT building" list in the PRD without explicit approval, even if they seem like natural next steps.
