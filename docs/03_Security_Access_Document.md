# Security & Access Document
## AI Camera Composition Assistant

---

### 1. Authentication Method

**None, by design.** The app is single-user, fully on-device, with no backend to authenticate against. There is no login screen, no email/password, no OAuth flow in v1. This isn't a gap — it's the correct choice for an app with no server and no multi-device sync requirement yet.

*Revisit this if/when:* you add cloud sync of shot history across devices, or a community/sharing feature. At that point, a simple option like Google Sign-In would be the lowest-friction choice, since your target Android users almost certainly already have a Google account on-device.

### 2. User Roles & Permissions

Only one role exists: **the device owner.** There's no admin panel, no guest mode, no multi-tenant data to separate. Nothing in the app reads or writes data belonging to any other user, because there is no concept of "other users" in this architecture.

### 3. Row-Level Security Rules

Not applicable for v1 — there is no database with multiple users' data to isolate. Noting here for future-proofing: if a v2 cloud-sync feature is added (e.g. via Supabase or Firebase), the rule to implement then is straightforward — each user's shot history and settings should only be readable/writable by that user's own authenticated session, never by others.

### 4. Error Handling

| Failure point | Behavior |
|---|---|
| Camera permission denied | Show a clear explanation screen with a button that deep-links to the app's system permission settings |
| Detection model fails to load (corrupted asset, unsupported device) | Fall back gracefully to grid-only mode (rule of thirds guide with no subject detection) rather than crashing |
| No subject detected in frame | Show the plain grid with no directional cue — don't show an error, since "nothing to detect yet" is a normal state, not a failure |
| Frame processing exceeds time budget | Skip that frame and process the next one; never block or freeze the camera preview |
| Storage full when saving a photo | Show a toast/snackbar with a clear message before the save attempt fails silently |
| Interruption mid-session (incoming call, app backgrounded) | Pause the analysis pipeline cleanly via lifecycle-aware CameraX bindings; resume on return without restarting the whole camera session |

### 5. Edge Cases

- **Multiple subjects detected:** prioritize the largest bounding box (closest/most prominent subject) as the default; a v2 "tap to select subject" interaction can override this.
- **Extremely low light:** detection confidence will drop — set a minimum confidence threshold below which the app shows the grid only, rather than giving unreliable directional guidance.
- **Device rotated to landscape:** grid and rule-of-thirds math must recompute against the new frame dimensions, not assume portrait.
- **Low-end device thermal throttling:** if frame-processing time trends upward over a session, dynamically reduce detection frequency (e.g. every 4th frame instead of every 2nd) rather than letting the UI stutter.
- **Empty/blank frame (lens covered, pointed at a wall):** treat as "no subject detected," not as an error state.
- **Rapid app backgrounding/foregrounding:** ensure the CameraX lifecycle binding doesn't leak the camera resource, which would block other apps (including the system camera) from accessing it.
