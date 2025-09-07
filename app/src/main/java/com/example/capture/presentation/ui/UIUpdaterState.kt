package com.example.capture.presentation.ui

import com.example.capture.core.ml.GestureType
import com.example.capture.performance.PerformanceStatus

data class UIUpdaterState(
    val currentGesture: GestureType = GestureType.NONE,
    val gestureConfidence: Float = 0f,
    val performanceStatus: PerformanceStatus = PerformanceStatus.OPTIMAL,
    val currentFPS: Double = 0.0,
    val isRecording: Boolean = false,
    val cameraFacing: Int = 0,
    val zoomRatio: Float = 1.0f,
    val lastAction: String? = null,
    val actionSuccess: Boolean = false,
    val actionMessage: String? = null,
    val showActionFeedback: Boolean = false,
    val showCooldown: Boolean = false,
    val cooldownMessage: String? = null,
    val lastGestureUpdate: Long = 0L,
    val lastCameraUpdate: Long = 0L,
    val lastActionUpdate: Long = 0L,
    val lastCooldownUpdate: Long = 0L
)
