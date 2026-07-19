# Product Requirements Document
## AI Camera Composition Assistant (working name: "Frame" — pick your own before launch, "Frame" is heavily used already)

---

### 1. Problem Statement

Most people take poorly composed photos not because their phone's camera is bad, but because they don't know *where to point it*. Rule of thirds, subject-to-frame ratio, horizon leveling — these are learnable skills, but nobody teaches them in the moment they matter: while you're actually holding the phone up.

Existing solutions solve adjacent problems but not this one cleanly:
- Cloud-based coaching (e.g. Google Pixel's Camera Coach) works, but is slow (round-trips to a server, sometimes 1-2 minutes) and fails without a strong signal — a dealbreaker for budget-device users on patchy mobile data, which describes a large share of the Indian smartphone market.
- Generative/enhancement apps fix photos *after* the fact (filters, upscaling, auto-retouch) rather than helping the user get it right at capture time.
- Several indie apps (LensMate, WayShot, PoseMe, ComposeAI) already do real-time composition coaching well for portraits/selfies specifically, but none position themselves around **fully offline, works-on-any-device** as the core value prop.

The gap: a composition coach that works instantly, on-device, with zero internet dependency, on a mid-range or budget Android phone.

### 2. Target Users

- **Primary:** Social media content creators and small e-commerce sellers (India-focused) shooting product photos, food photos, or lifestyle content on budget-to-mid-range Android phones, often on limited mobile data.
- **Secondary:** Photography beginners/hobbyists who want to learn composition principles by doing, not by reading.
- **Tertiary:** Anyone handing their phone to a friend/family member to take a photo of them — the app should be usable by someone with zero photography knowledge.

### 3. Product Vision

A pocket-sized photography coach that works instantly, offline, on any Android phone — no signal, no subscription server round-trip, no waiting.

### 4. Core Features (v1 scope)

| Feature | Description | Priority |
|---|---|---|
| Live subject detection | Detects the main subject/face in the camera preview in real time | Must-have |
| Rule-of-thirds guidance | Compares subject position to the 3×3 grid intersections and tells the user which way to move | Must-have |
| Visual overlay (grid + directional cue) | On-screen grid lines and an arrow/indicator showing which way to move the camera | Must-have |
| Capture & save | Standard shutter button, saves to device gallery | Must-have |
| Haptic "good zone" feedback | Short vibration when the frame is well-composed, so the user doesn't have to stare at text | Should-have |
| Horizon level indicator | Uses the accelerometer to flag a tilted horizon | Should-have |
| Settings (toggle grid/haptics) | Basic customization | Should-have |
| Audio cues | Spoken/tone-based directional guidance for hands-free or accessibility use | Nice-to-have (v2) |
| Exposure warning | Flags badly over/underexposed frames via histogram check | Nice-to-have (v2) |
| Niche modes (product photo, portrait, landscape presets) | Different rule weighting per scenario | Nice-to-have (v2) |
| Learned aesthetic score | ML model trained on an aesthetics dataset, layered on top of the rule engine | Nice-to-have (v2+) |

### 5. App Flow

1. User opens the app → camera permission requested (first launch only)
2. Live camera preview appears with a 3×3 grid overlay
3. App detects subject in real time → shows a directional indicator (e.g. arrow, "move left," subtle highlight) toward the nearest good composition point
4. When the frame crosses into a "good zone," the grid/indicator changes state (e.g. turns green) and the phone gives a short haptic pulse
5. User taps the shutter button → photo is captured and saved to the gallery
6. (Optional) Quick post-capture preview with a "retake" option

### 6. Success Metrics

- Frame-processing latency (target: under ~66ms/frame, i.e. usable at 15fps minimum)
- % of captured shots taken while in a "good zone" vs. total shots (proxy for whether guidance is actually being followed)
- Day-1 / Day-7 retention
- Average session length
- Crash-free session rate
- Battery/thermal impact per 5-minute session (should not cause visible throttling on a mid-range device)

### 7. What We Are Deliberately NOT Building in v1

- No cloud AI calls of any kind — everything runs on-device
- No image enhancement, filters, or generative editing
- No user accounts, login, or cloud sync
- No iOS version (Android-first, given target hardware and your own dev environment)
- No learned aesthetic-scoring model (rule-based only for v1)
- No video/reels composition coaching (photo only)
- No social sharing features
