package com.example.capture.data.model

import com.example.capture.domain.model.GestureResult
import com.example.capture.domain.model.GestureType

data class GestureDetectionState(
    val isInitialized: Boolean = false,
    val isDetecting: Boolean = false,
    val isEnabled: Boolean = true,
    val currentGesture: GestureResult = GestureResult(GestureType.NONE, 0f),
    val detectionCount: Long = 0L
)
