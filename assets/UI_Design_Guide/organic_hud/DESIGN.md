---
name: Organic HUD
colors:
  surface: '#111413'
  surface-dim: '#111413'
  surface-bright: '#373a39'
  surface-container-lowest: '#0c0f0e'
  surface-container-low: '#191c1b'
  surface-container: '#1d201f'
  surface-container-high: '#272b2a'
  surface-container-highest: '#323534'
  on-surface: '#e1e3e1'
  on-surface-variant: '#bfc9c5'
  inverse-surface: '#e1e3e1'
  inverse-on-surface: '#2e3130'
  outline: '#899390'
  outline-variant: '#404946'
  surface-tint: '#97d2c7'
  primary: '#97d2c7'
  on-primary: '#003731'
  primary-container: '#73ada3'
  on-primary-container: '#00413a'
  inverse-primary: '#2d685f'
  secondary: '#a7cfc5'
  on-secondary: '#103630'
  secondary-container: '#284d46'
  on-secondary-container: '#96bdb4'
  tertiary: '#fdb5a2'
  on-tertiary: '#502417'
  tertiary-container: '#d49280'
  on-tertiary-container: '#5a2c1f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#b2eee3'
  primary-fixed-dim: '#97d2c7'
  on-primary-fixed: '#00201c'
  on-primary-fixed-variant: '#0e5048'
  secondary-fixed: '#c3ebe1'
  secondary-fixed-dim: '#a7cfc5'
  on-secondary-fixed: '#00201b'
  on-secondary-fixed-variant: '#284d46'
  tertiary-fixed: '#ffdbd1'
  tertiary-fixed-dim: '#fdb5a2'
  on-tertiary-fixed: '#350f05'
  on-tertiary-fixed-variant: '#6b392b'
  background: '#111413'
  on-background: '#e1e3e1'
  surface-variant: '#323534'
  canvas-dark: '#0D0A0B'
  surface-deep: '#0E1715'
  success-ice: '#9AC6BB'
  warning-plum: '#30252A'
  error-plum: '#29181D'
  text-primary: '#E6ECEB'
  text-secondary: '#548173'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '400'
    lineHeight: '1.5'
  body-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.5'
  label-mono:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  label-mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: '1.2'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 1.5rem
  gutter: 1rem
  control-gap: 0.75rem
  safe-area-bottom: 2rem
---

# FrameCoach Design System

## 1. Color Palette (Organic, Low-Glare Dark Theme)
| Role | Color Name | Hex Code | Purpose |
| :--- | :--- | :--- | :--- |
| **Canvas Dark (Base)** | Obsidian Black | `#0D0A0B` | Edge chrome, background behind viewports |
| **Surface (Deep)** | Spruce Black | `#0E1715` | Floating bottom bars, settings panels, overlays |
| **Surface (Medium)** | Forest Spruce | `#20453E` | Unselected toggles, secondary container backgrounds |
| **Accent / Interactive** | Eucalyptus Sage | `#73ADA3` | Primary active states, buttons, shutter button outline |
| **Success State** | Ice Eucalyptus | `#9AC6BB` | Camera grid alignment, "Good Zone" cues |
| **Warning** | Warm Plum | `#30252A` | Warning overlays, accelerometer off-level cues |
| **Error / Alert** | Deep Plum | `#29181D` | Critical error, disabled states |
| **Text (Primary)** | Clean Sage-White | `#E6ECEB` | Headers, labels, primary readable UI text |
| **Text (Secondary)** | Eucalyptus Green | `#548173` | Subtitles, disabled tabs, low-emphasis metadata |

## 2. Typography
- **Display/Headers:** `Satoshi` (fallback: `Inter`) - Track-tight, medium weight.
- **Numerical Data/HUD:** `JetBrains Mono` (fallback: `monospace`) - For real-time composition scores and levels.
- **Body/Controls:** `Satoshi` or `Inter` (18–20sp) - Instant readability.

## 3. Component Styles
- **Buttons:** Large, tactile. Shutter button uses a `#73ADA3` outer ring.
- **Overlays:** Camera grid lines are thin (1dp) using `#20453E` at 40% opacity, transitioning to `#9AC6BB` on success.
- **Controls:** Floating on top of the viewport with bottom-heavy ergonomics.
- **Bottom Sheet:** 24dp top corner radius, `#0E1715` background.
