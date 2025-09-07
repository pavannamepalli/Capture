
package com.example.capture.data.repository

import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.example.capture.core.error.AppError
import com.example.capture.core.result.Result
import com.example.capture.data.datasource.CameraDataSource
import com.example.capture.data.datasource.FileSystemDataSource
import com.example.capture.domain.model.CameraAction
import com.example.capture.domain.model.CameraActionResult
import com.example.capture.domain.model.CameraState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraRepository(
    private val cameraDataSource: CameraDataSource,
    private val fileSystemDataSource: FileSystemDataSource
) {
    
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
        fun initializeCamera(
        camera: Camera?,
        imageCapture: ImageCapture?,
        videoCapture: VideoCapture<Recorder>?
    ): Result<Unit> {
        return try {
            cameraDataSource.setCamera(camera, imageCapture, videoCapture)
            _cameraState.value = _cameraState.value.copy(
                isInitialized = true
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraInitializationFailed
            )
        }
    }
    
        suspend fun executeCameraAction(action: CameraAction): Result<CameraActionResult> {
        return try {
            _cameraState.value = _cameraState.value.copy(
                isLoading = true,
                lastAction = action
            )
            
            val result = when (action) {
                CameraAction.CAPTURE_PHOTO -> capturePhoto()
                CameraAction.START_VIDEO_RECORDING -> startVideoRecording()
                CameraAction.STOP_VIDEO_RECORDING -> stopVideoRecording()
                CameraAction.SWITCH_CAMERA -> switchCamera()
                CameraAction.OPEN_GALLERY -> openGallery()
                CameraAction.ZOOM_IN -> zoomIn()
                CameraAction.ZOOM_OUT -> zoomOut()
                CameraAction.TOGGLE_FLASH -> toggleFlash()
            }
            
            _cameraState.value = _cameraState.value.copy(
                isLoading = false,
                lastActionResult = result.getOrNull()
            )
            
            result
        } catch (e: Exception) {
            val error = AppError.CameraError.CameraOperationFailed(
                operation = action.name,
                cause = e
            )
            _cameraState.value = _cameraState.value.copy(
                isLoading = false
            )
            Result.Error(error)
        }
    }
    
        private suspend fun capturePhoto(): Result<CameraActionResult> {
        return try {
            val photoFile = fileSystemDataSource.createPhotoFile()
            if (photoFile == null) {
                return Result.Error(
                    AppError.FileSystemError.FileCreationFailed
                )
            }
            
            val result = cameraDataSource.capturePhoto(photoFile)
            if (result.isSuccess) {
                                fileSystemDataSource.savePhotoToGallery(photoFile)
                _cameraState.value = _cameraState.value.copy(
                    lastPhotoUri = Uri.fromFile(photoFile)
                )
            }
            
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraCaptureFailed
            )
        }
    }
    
        private suspend fun startVideoRecording(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.startVideoRecording()
            if (result.isSuccess) {
                _cameraState.value = _cameraState.value.copy(
                    isRecording = true
                )
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraRecordingFailed
            )
        }
    }
    
        private suspend fun stopVideoRecording(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.stopVideoRecording()
            if (result.isSuccess) {
                _cameraState.value = _cameraState.value.copy(
                    isRecording = false
                )
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraRecordingFailed
            )
        }
    }
    
        private suspend fun switchCamera(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.switchCamera()
            if (result.isSuccess) {
                val newFacing = cameraDataSource.getCurrentCameraFacing()
                _cameraState.value = _cameraState.value.copy(
                    cameraFacing = newFacing
                )
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "switch_camera",
                    cause = e
                )
            )
        }
    }
    
        private suspend fun openGallery(): Result<CameraActionResult> {
        return cameraDataSource.openGallery()
    }
    
        private suspend fun zoomIn(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.zoomIn()
            if (result.isSuccess) {
                val newZoomRatio = result.getOrNull()?.data as? Float
                if (newZoomRatio != null) {
                    _cameraState.value = _cameraState.value.copy(
                        zoomRatio = newZoomRatio
                    )
                }
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "zoom_in",
                    cause = e
                )
            )
        }
    }
    
        private suspend fun zoomOut(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.zoomOut()
            if (result.isSuccess) {
                val newZoomRatio = result.getOrNull()?.data as? Float
                if (newZoomRatio != null) {
                    _cameraState.value = _cameraState.value.copy(
                        zoomRatio = newZoomRatio
                    )
                }
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "zoom_out",
                    cause = e
                )
            )
        }
    }
    
        private suspend fun toggleFlash(): Result<CameraActionResult> {
        return try {
            val result = cameraDataSource.toggleFlash()
            if (result.isSuccess) {
                val flashEnabled = result.getOrNull()?.data as? Boolean
                if (flashEnabled != null) {
                    _cameraState.value = _cameraState.value.copy(
                        flashEnabled = flashEnabled
                    )
                }
            }
            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "toggle_flash",
                    cause = e
                )
            )
        }
    }
    
        fun resetGalleryState() {
        _cameraState.value = _cameraState.value.copy(
            showGallery = false
        )
    }
    
        fun cleanup() {
        cameraDataSource.cleanup()
    }
}
