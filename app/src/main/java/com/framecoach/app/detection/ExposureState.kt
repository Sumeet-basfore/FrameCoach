package com.framecoach.app.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state holder for exposure analysis results (T12).
 *
 * The camera pipeline writes exposure data here; the UI reads it via
 * [result] StateFlow.  Follows the same pattern as [CompositionState].
 */
object ExposureState {

    private val _result: MutableStateFlow<ExposureResult> = MutableStateFlow(
        ExposureResult(meanLuminance = 0.5f, isUnderexposed = false, isOverexposed = false)
    )

    /** Latest exposure analysis result. */
    val result: StateFlow<ExposureResult> = _result.asStateFlow()

    /**
     * Update the exposure analysis result.
     */
    fun update(exposureResult: ExposureResult) {
        _result.value = exposureResult
    }
}
