
package com.example.capture.data.repository

import android.graphics.Bitmap
import com.example.capture.core.error.AppError
import com.example.capture.core.result.Result
import com.example.capture.data.datasource.GestureDataSource
import com.example.capture.domain.model.GestureResult
import com.example.capture.domain.model.GestureType
import com.example.capture.data.model.PerformanceStats
import com.example.capture.data.model.GestureDetectionState
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GestureRepository(
    private val gestureDataSource: GestureDataSource
) {
    
    private val _gestureState = MutableStateFlow(GestureDetectionState())
    val gestureState: StateFlow<GestureDetectionState> = _gestureState.asStateFlow()
    
        suspend fun initializeGestureDetection(): Result<Unit> {
        return try {
            gestureDataSource.initialize()
            _gestureState.value = _gestureState.value.copy(
                isInitialized = true,
                isDetecting = false
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                AppError.GestureError.ModelInitializationFailed
            )
        }
    }
    

        suspend fun processLandmarks(landmarkResult: HandLandmarkerResult): Result<GestureResult> {
        return try {
            if (!_gestureState.value.isInitialized) {
                return Result.Error(
                    AppError.GestureError.ModelNotLoaded
                )
            }
            
            _gestureState.value = _gestureState.value.copy(
                isDetecting = true,
                detectionCount = _gestureState.value.detectionCount + 1
            )
            
            val result = gestureDataSource.processLandmarks(landmarkResult)
            
            if (result.isSuccess) {
                val gestureResult = result.getOrNull() ?: GestureResult(GestureType.NONE, 0f)
                _gestureState.value = _gestureState.value.copy(
                    currentGesture = gestureResult,
                    isDetecting = false
                )
            } else {
                _gestureState.value = _gestureState.value.copy(
                    isDetecting = false
                )
            }
            
            result
        } catch (e: Exception) {
            _gestureState.value = _gestureState.value.copy(
                isDetecting = false
            )
            Result.Error(
                AppError.GestureError.GestureDetectionFailed(e)
            )
        }
    }
    
        fun cleanup() {
        gestureDataSource.cleanup()
    }
}



