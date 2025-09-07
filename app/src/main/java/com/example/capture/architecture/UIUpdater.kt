package com.example.capture.architecture

import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.GestureType
import com.example.capture.presentation.ui.GestureFeedback
import com.example.capture.presentation.ui.UIUpdaterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UIUpdater {


    private val _uiState = MutableStateFlow(UIUpdaterState())
    val uiState: StateFlow<UIUpdaterState> = _uiState.asStateFlow()

    private val _gestureFeedback = MutableStateFlow(GestureFeedback())
    val gestureFeedback: StateFlow<GestureFeedback> = _gestureFeedback.asStateFlow()

    fun updateGestureResult(gestureResult: GestureResult) {
        try {
            _gestureFeedback.value = GestureFeedback(
                gestureType = gestureResult.gestureType,
                confidence = gestureResult.confidence,
                timestamp = gestureResult.timestamp,
                isVisible = gestureResult.gestureType != GestureType.NONE && gestureResult.confidence > 0.7f
            )

            _uiState.value = _uiState.value.copy(
                currentGesture = gestureResult.gestureType,
                gestureConfidence = gestureResult.confidence,
                lastGestureUpdate = System.currentTimeMillis()
            )


        } catch (_: Exception) {
        }
    }


    fun updateCameraState(
        isRecording: Boolean, cameraFacing: Int, zoomRatio: Float
    ) {
        try {
            _uiState.value = _uiState.value.copy(
                isRecording = isRecording,
                cameraFacing = cameraFacing,
                zoomRatio = zoomRatio,
                lastCameraUpdate = System.currentTimeMillis()
            )


        } catch (_: Exception) {
        }
    }

    fun updateActionFeedback(
        action: String, success: Boolean, message: String? = null
    ) {
        try {
            _uiState.value = _uiState.value.copy(
                lastAction = action,
                actionSuccess = success,
                actionMessage = message,
                showActionFeedback = true,
                lastActionUpdate = System.currentTimeMillis()
            )

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _uiState.value = _uiState.value.copy(showActionFeedback = false)
            }, 3000)


        } catch (_: Exception) {
        }
    }


}

