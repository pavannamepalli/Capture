package com.example.capture.presentation.ui

import com.example.capture.core.ml.GestureType

data class GestureFeedback(
    val gestureType: GestureType = GestureType.NONE,
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isVisible: Boolean = false
)
