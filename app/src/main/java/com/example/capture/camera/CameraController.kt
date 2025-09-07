package com.example.capture.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import com.example.capture.domain.model.CameraState
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
) {

    private val _cameraState = MutableStateFlow(CameraState())

    private var currentCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var isRecording = false

    private var currentCameraFacing = CameraSelector.LENS_FACING_FRONT
    private var currentZoomRatio = 1.0f

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun setCamera(
        camera: Camera?,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null
    ) {
        currentCamera = camera
        this.imageCapture = imageCapture
        this.videoCapture = videoCapture
        updateCameraState()
    }

    private fun updateCameraState() {
        val currentFlashState = currentCamera?.cameraInfo?.torchState?.value == TorchState.ON
        _cameraState.value = _cameraState.value.copy(
            cameraFacing = currentCameraFacing,
            zoomRatio = currentZoomRatio,
            isRecording = isRecording,
            flashEnabled = currentFlashState
        )
    }



    fun cleanup() {
        currentRecording?.stop()
        currentRecording = null
        isRecording = false
        currentCamera = null
        imageCapture = null
        cameraExecutor.shutdown()
    }
}

