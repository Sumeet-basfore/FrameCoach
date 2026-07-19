# GEMINI.md

## Project Blueprint: AI Camera Composition Assistant (Android)

## Role & Approach

Act as a senior Android engineer implementing an already-scoped spec, not as a product designer re-architecting it. The planning docs referenced below reflect decisions already made — follow them faithfully rather than proposing alternative stacks, adding features, or "improving" the scope mid-implementation. If something in a ticket seems to require deviating from the architecture below, stop and ask before proceeding, rather than deciding autonomously.

This project is also being built with Claude Code alongside Antigravity, using a parallel `CLAUDE.md` at the repo root. Both files encode the same constraints intentionally — if you ever see the two disagree, treat that as a bug to flag, not a decision to resolve unilaterally.

## What this app does

Real-time, fully offline camera-composition coach for Android. Analyzes the live camera feed and tells the user how to reposition the camera for a better shot — rule of thirds, subject framing, horizon level. It does **not** generate, enhance, retouch, or edit images. Coaching only, never image manipulation.

## Reference docs

Read the relevant doc before working in that area rather than re-deriving decisions already made:

- `01_PRD_AI_Camera_Composition_Assistant.md` — problem, users, features, explicit non-goals
- `02_Technical_Architecture_Document.md` — stack, folder structure, schema
- `03_Security_Access_Document.md` — auth (none), error handling, edge cases
- `04_Frontend_Specification_Document.md` — colors, typography, component styles
- `05_Feature_Ticket_List.md` — build order; work tickets in this sequence

## Tech stack — do not deviate without asking first

- Kotlin, Android, min SDK 24
- CameraX (`ImageAnalysis` use case) for camera preview and frame access
- MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`) — EfficientDet-Lite0 for general object detection, Face Detector task for portrait mode
- Jetpack Compose (`Canvas`) for the grid/overlay UI, layered over `PreviewView`
- Kotlin Coroutines for off-main-thread frame processing
- No backend, no server, no cloud AI calls, no login — see Hard Constraints below

## Build/run

```
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
```
Unit-test the rules engine (`rules/` package) directly — it's pure input→output logic and shouldn't require a device or emulator to verify.

## Project layout

```
app/src/main/java/com/yourpackage/framecoach/
  camera/      # CameraX setup, ImageAnalysis pipeline
  detection/   # MediaPipe wrapper — model loading, inference, bounding boxes out
  rules/       # Composition rules engine — pure functions, no Android dependencies
  ui/overlay/  # Compose Canvas grid + directional indicator
  ui/capture/  # Shutter button, capture flow, save-to-gallery
  ui/settings/ # Grid/haptics toggles
  sensors/     # Accelerometer wrapper for horizon leveling
```

## Hard constraints — never violate these without explicit sign-off

- **No network calls in the detection/rules pipeline.** Offline-first is the product's core differentiation versus cloud-based competitors (Google's own Pixel "Camera Coach" is the cautionary example here — slow and unreliable specifically because it round-trips to a server). If a task seems to need a network call, flag it instead of adding it.
- **No login/auth flow in v1.** Single-user, on-device, no accounts.
- **No image generation, enhancement, or auto-retouch.** This app advises on framing before capture. It never modifies the captured photo afterward.
- **Detection must never block the camera preview thread.** Run inference via coroutines off the main thread; skip a frame rather than stall one.
- **No custom-trained ML models in v1.** Use MediaPipe's pre-trained tasks and rule-based heuristics only. A learned aesthetic-scoring model is explicitly deferred to v2+ (see PRD).
- **Detection model must be INT8-quantized.** Float variants measured at 300ms+/frame on Snapdragon 680-class budget hardware — unusable for real-time use. Ship INT8 only.
- **Directional guidance must never oscillate or demand unsafe/impossible repositioning.** Require a minimum-improvement threshold before flipping a suggestion, and bound the maximum repositioning ever suggested — a documented failure mode in Google's own Camera Coach.
- **Use the Android thermal status API for adaptive throttling**, not just frame-processing time trends — `PowerManager.addThermalStatusListener` (API 29+) gives the OS's actual signal.

## Conventions

- Keep the rules engine (`rules/`) as pure functions: bounding box + frame dimensions in, directional suggestion + "good zone" boolean out. No Android framework dependencies in this package.
- Work tickets in the order given in `05_Feature_Ticket_List.md`. Don't start a "should-have" or "nice-to-have" ticket while a "must-have" ticket is incomplete.
- Before implementing a ticket, restate its acceptance criteria and a one- or two-sentence implementation plan before writing code.
- Match colors/typography to `04_Frontend_Specification_Document.md` exactly (Catppuccin Mocha–based palette) rather than defaulting to Material's stock colors.

## What not to build yet

Cloud sync, user accounts, video/reels coaching, social sharing, learned aesthetic scoring, niche presets beyond "general" and "portrait." All deferred to v2 per the PRD — treat any task that seems to require one of these as a signal to ask, not build around.
