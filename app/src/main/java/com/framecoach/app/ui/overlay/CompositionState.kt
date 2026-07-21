package com.framecoach.app.ui.overlay

import com.framecoach.app.rules.CompositionSuggestion
import com.framecoach.app.rules.CompositionRules
import com.framecoach.app.detection.BoundingBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state holder for composition analysis results.
 *
 * The camera pipeline writes detected boxes here; the overlay composable
 * reads them via [suggestion] StateFlow. This avoids coupling the camera and
 * overlay packages directly.
 */
object CompositionState {

    private val _suggestion: MutableStateFlow<CompositionSuggestion> = MutableStateFlow(CompositionSuggestion.GOOD)

    /** Latest composition analysis result. */
    val suggestion: StateFlow<CompositionSuggestion> = _suggestion.asStateFlow()

    fun update(boxes: List<BoundingBox>, style: String = "rule_of_thirds", mode: String = "general", sensitivity: Float = 0.42f) {
        val suggestion = CompositionRules.analyseMulti(boxes, style, mode, _suggestion.value, sensitivity)
        _suggestion.value = suggestion
    }
}