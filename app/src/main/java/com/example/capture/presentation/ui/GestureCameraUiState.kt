package com.example.capture.presentation.ui

import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.GestureType
import com.example.capture.domain.model.CameraActionResult
import com.example.capture.domain.model.CameraState

data class GestureCameraUiState(
    val cameraInitialized: Boolean = false,
    val gestureDetectionInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isDetecting: Boolean = false,
    val gestureDetectionEnabled: Boolean = true,
    val cameraState: CameraState? = null,
    val currentGesture: GestureResult = GestureResult(GestureType.NONE, 0f),
    val lastActionResult: CameraActionResult? = null,
    val showActionFeedback: Boolean = false,
    val showCooldown: Boolean = false,
    val cooldownMessage: String? = null
)
