package com.example.capture.core.ml

enum class GestureType {
    OPEN_PALM, PEACE_SIGN, THUMBS_UP, OK_SIGN, PINCH_ZOOM_IN, PINCH_ZOOM_OUT, THREE_FINGERS_UP, NONE
}

data class GestureResult(
    val gestureType: GestureType,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
