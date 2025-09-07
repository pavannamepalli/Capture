
package com.example.capture.data.datasource

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.example.capture.core.result.Result
import com.example.capture.domain.model.CameraActionResult
import java.io.File

interface CameraDataSource {
    
        fun setCamera(
        camera: Camera?,
        imageCapture: ImageCapture?,
        videoCapture: VideoCapture<Recorder>?
    )
    
        suspend fun capturePhoto(photoFile: File): Result<CameraActionResult>
    
        suspend fun startVideoRecording(): Result<CameraActionResult>
    
        suspend fun stopVideoRecording(): Result<CameraActionResult>
    
        suspend fun switchCamera(): Result<CameraActionResult>
    
        suspend fun zoomIn(): Result<CameraActionResult>
    
        suspend fun zoomOut(): Result<CameraActionResult>
    
        suspend fun toggleFlash(): Result<CameraActionResult>
    
        suspend fun openGallery(): Result<CameraActionResult>
    
        fun getCurrentCameraFacing(): Int
    
        fun cleanup()
}
