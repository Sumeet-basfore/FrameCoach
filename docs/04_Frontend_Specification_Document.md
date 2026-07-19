# Frontend Specification Document
## AI Camera Composition Assistant

---

### 1. Color Palette

A dark, low-glare theme suits this app well beyond aesthetics — bright UI chrome around a camera viewfinder is genuinely distracting while shooting. Given you already run Catppuccin Mocha across your desktop setup (kitty, vxwm, etc.), carrying that theme into the app gives you a coherent personal brand across your whole toolchain, and it's a legitimately good fit for a camera UI:

| Role | Color | Hex |
|---|---|---|
| Background (base) | Base | `#1E1E2E` |
| Background (deeper, e.g. bottom sheets) | Mantle | `#181825` |
| Primary text | Text | `#CDD6F4` |
| Muted/secondary text | Subtext0 | `#A6ADC8` |
| Accent / primary interactive (shutter button, active toggle) | Mauve | `#CBA6F7` |
| "Good zone" positive feedback (grid turns this color) | Green | `#A6E3A1` |
| "Needs adjustment" state | Peach | `#FAB387` |
| Warning (low light, low confidence) | Yellow | `#F9E2AF` |
| Error state | Red | `#F38BA8` |
| Grid lines (neutral, low-opacity over camera feed) | Overlay1 | `#7F849C` at ~40% opacity |

Keep the grid lines and non-critical overlay elements at reduced opacity — they need to be visible against any scene without ever visually competing with the actual photo the user is composing.

### 2. Typography

- **Primary UI font:** Roboto (Android system default) or Inter — optimize for legibility at a glance, since users will be reading directional cues quickly while holding a phone steady.
- **Monospace/data accents:** JetBrains Mono (Nerd Font variant if you want the icon glyphs) for anything numeric or technical-feeling — composition score, exposure values, coordinates in a future debug/pro mode. This is a nice-to-have detail, not load-bearing for v1.
- **Sizing:** directional cue text should be large enough to read in peripheral vision while looking at the subject, not the text — err toward 18–20sp minimum for any on-screen guidance text.

### 3. Component Styles

- **Shutter button:** large circular button, Mauve accent, bottom-center, respects one-handed reachability (thumb zone)
- **Directional indicator:** a simple arrow or pulsing dot at the edge of frame pointing toward the nearest "good" composition point — avoid literal text like "move left" as the primary cue; reserve text for a settings-toggleable "beginner mode"
- **Grid overlay:** thin lines (1–2dp), Overlay1 at low opacity, animates to Green when the current frame crosses into a good composition zone
- **Bottom sheet (mode selection — portrait/product/general):** Mantle background, rounded top corners, swipe-up gesture
- **Settings screen:** standard Material list items, toggle switches for grid/haptics, no unnecessary nested navigation

### 4. Spacing & Layout Rules

- Base unit: 8dp grid (standard Material spacing) — 8/16/24/32dp increments for all padding and margins
- Camera preview always fills the available viewport edge-to-edge; UI chrome floats on top, never letterboxes the preview
- Bottom controls zone: minimum 96dp height to keep the shutter button comfortably tappable

### 5. API & Integration Spec

**None for v1.** There are no third-party services to integrate — no Stripe, no Firebase, no OpenAI calls. This section is intentionally empty because the app's core value is that it works with zero network dependency.

*For v2, if added:*
| Service | Purpose | Notes |
|---|---|---|
| Firebase Analytics (optional) | Understand which composition rules users actually follow | Should be opt-in, clearly disclosed — this is a photography app, not a data-collection app |
| Cloud sync backend (e.g. Supabase) | Cross-device shot history | Only if user demand justifies the added complexity and the auth/security work in Section 1 of the Security & Access doc |
