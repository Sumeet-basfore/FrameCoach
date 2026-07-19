package com.framecoach.app.ui.camera

/**
 * Representation of the camera permission state.
 *
 * - [Unknown]: not yet checked; the launcher is ready but hasn't been invoked.
 * - [Granted]: permission is held — camera preview can be shown.
 * - [Denied]: user denied the request (but `shouldShowRequestPermissionRationale` is true,
 *   meaning a re-request with a rationale is possible).
 * - [PermanentlyDenied]: user denied and checked "Don't ask again" — the only way
 *   forward is a manual deep-link to system settings.
 */
enum class CameraPermissionState {
    Unknown,
    Granted,
    Denied,
    PermanentlyDenied,
}
