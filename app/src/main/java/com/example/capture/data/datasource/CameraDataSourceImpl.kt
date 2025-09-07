package com.example.capture.data.datasource

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.example.capture.core.error.AppError
import com.example.capture.core.result.Result
import com.example.capture.domain.model.CameraAction
import com.example.capture.domain.model.CameraActionResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraDataSourceImpl(
    private val context: Context
) : CameraDataSource {

    private var currentCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var isRecording = false

    private var currentCameraFacing = CameraSelector.LENS_FACING_FRONT
    private var currentZoomRatio = 1.0f
    private val minZoomRatio = 1.0f
    private val maxZoomRatio = 10.0f

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun setCamera(
        camera: Camera?, imageCapture: ImageCapture?, videoCapture: VideoCapture<Recorder>?
    ) {
        currentCamera = camera
        this.imageCapture = imageCapture
        this.videoCapture = videoCapture

        camera?.cameraInfo?.torchState?.observeForever { torchState ->
        }
    }

    override suspend fun capturePhoto(photoFile: File): Result<CameraActionResult> {
        return try {
            val imageCapture = imageCapture ?: return Result.Error(
                AppError.CameraError.CameraOperationFailed("Image capture not initialized")
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            val result = suspendCoroutine<Result<CameraActionResult>> { continuation ->
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            continuation.resume(
                                Result.Success(
                                    CameraActionResult(
                                        action = CameraAction.CAPTURE_PHOTO,
                                        success = true,
                                        message = "Photo captured successfully",
                                        data = photoFile.absolutePath
                                    )
                                )
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            continuation.resume(
                                Result.Error(
                                    AppError.CameraError.CameraOperationFailed(
                                        operation = "capturePhoto", cause = exception
                                    )
                                )
                            )
                        }
                    })
            }

            result
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "capturePhoto", cause = e
                )
            )
        }
    }

    override suspend fun startVideoRecording(): Result<CameraActionResult> {
        return try {
            if (isRecording) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Already recording")
                )
            }

            val videoCapture = videoCapture ?: return Result.Error(
                AppError.CameraError.CameraOperationFailed("Video capture not initialized")
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val videoFileName = "GestureCamera_$timestamp.mp4"

            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_MOVIES + "/GestureCamera"
                )
            }

            val outputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            currentRecording =
                videoCapture.output.prepareRecording(context, outputOptions).withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                isRecording = true
                            }

                            is VideoRecordEvent.Finalize -> {
                                if (event.hasError()) {
                                    isRecording = false
                                }
                            }

                            else -> {
                            }
                        }
                    }

            Result.Success(
                CameraActionResult(
                    action = CameraAction.START_VIDEO_RECORDING,
                    success = true,
                    message = "Video recording started",
                    data = videoFileName
                )
            )
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraRecordingFailed
            )
        }
    }

    override suspend fun stopVideoRecording(): Result<CameraActionResult> {
        return try {
            if (!isRecording) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Not currently recording")
                )
            }

            val recording = currentRecording
            if (recording != null) {
                recording.stop()
                isRecording = false
                currentRecording = null


                Result.Success(
                    CameraActionResult(
                        action = CameraAction.STOP_VIDEO_RECORDING,
                        success = true,
                        message = "Video recording stopped and saved to gallery"
                    )
                )
            } else {
                Result.Error(
                    AppError.CameraError.CameraOperationFailed("No active recording to stop")
                )
            }
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraRecordingFailed
            )
        }
    }

    override suspend fun switchCamera(): Result<CameraActionResult> {
        return try {
            currentCameraFacing = if (currentCameraFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

            Result.Success(
                CameraActionResult(
                    action = CameraAction.SWITCH_CAMERA,
                    success = true,
                    message = "Camera switched to ${if (currentCameraFacing == CameraSelector.LENS_FACING_FRONT) "front" else "back"}"
                )
            )
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "switch_camera", cause = e
                )
            )
        }
    }

    override suspend fun zoomIn(): Result<CameraActionResult> {
        return try {
            val newZoomRatio = (currentZoomRatio + 0.5f).coerceAtMost(maxZoomRatio)

            if (newZoomRatio != currentZoomRatio) {
                currentZoomRatio = newZoomRatio
                currentCamera?.cameraControl?.setZoomRatio(currentZoomRatio)

                Result.Success(
                    CameraActionResult(
                        action = CameraAction.ZOOM_IN,
                        success = true,
                        message = "Zoomed in to ${String.format("%.1f", currentZoomRatio)}x",
                        data = currentZoomRatio
                    )
                )
            } else {
                Result.Error(
                    AppError.CameraError.CameraOperationFailed("Already at maximum zoom")
                )
            }
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "zoom_in", cause = e
                )
            )
        }
    }

    override suspend fun zoomOut(): Result<CameraActionResult> {
        return try {
            val newZoomRatio = (currentZoomRatio - 0.5f).coerceAtLeast(minZoomRatio)

            if (newZoomRatio != currentZoomRatio) {
                currentZoomRatio = newZoomRatio
                currentCamera?.cameraControl?.setZoomRatio(currentZoomRatio)

                Result.Success(
                    CameraActionResult(
                        action = CameraAction.ZOOM_OUT,
                        success = true,
                        message = "Zoomed out to ${String.format("%.1f", currentZoomRatio)}x",
                        data = currentZoomRatio
                    )
                )
            } else {
                Result.Error(
                    AppError.CameraError.CameraOperationFailed("Already at minimum zoom")
                )
            }
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "zoom_out", cause = e
                )
            )
        }
    }

    override suspend fun toggleFlash(): Result<CameraActionResult> {
        return try {
            val camera = currentCamera ?: return Result.Error(
                AppError.CameraError.CameraOperationFailed("Camera not initialized")
            )

            val cameraFacing = camera.cameraInfo.cameraSelector.lensFacing


            val hasCameraPermission =
                context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED


            if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Flash is only available on back camera. Please switch to back camera to use flash.")
                )
            }

            camera.cameraInfo.hasFlashUnit()

            camera.cameraControl

            if (!hasCameraPermission) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Camera permission not granted")
                )
            }


            val currentFlashMode = camera.cameraInfo.torchState.value

            val newFlashMode = if (currentFlashMode == androidx.camera.core.TorchState.ON) {
                androidx.camera.core.TorchState.OFF
            } else {
                androidx.camera.core.TorchState.ON
            }



            try {
                camera.cameraControl.enableTorch(newFlashMode == androidx.camera.core.TorchState.ON)
            } catch (e: Exception) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Failed to control flash: ${e.message}")
                )
            }

            Thread.sleep(500)
            val actualFlashState = camera.cameraInfo.torchState.value

            val stateChanged = actualFlashState == newFlashMode

            if (!stateChanged) {
                return Result.Error(
                    AppError.CameraError.CameraOperationFailed("Flash control failed - hardware may not be available or flash unit not detected")
                )
            }

            val flashStatus =
                if (newFlashMode == androidx.camera.core.TorchState.ON) "ON" else "OFF"

            Result.Success(
                CameraActionResult(
                    action = CameraAction.TOGGLE_FLASH,
                    success = true,
                    message = "Flash turned $flashStatus",
                    data = newFlashMode == androidx.camera.core.TorchState.ON
                )
            )
        } catch (e: Exception) {
            Result.Error(
                AppError.CameraError.CameraOperationFailed(
                    operation = "toggle_flash", cause = e
                )
            )
        }
    }

    override suspend fun openGallery(): Result<CameraActionResult> {
        return try {

            try {
                val samsungIntent =
                    context.packageManager.getLaunchIntentForPackage("com.sec.android.gallery3d")
                if (samsungIntent != null) {
                    samsungIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(samsungIntent)
                    return Result.Success(
                        CameraActionResult(
                            action = CameraAction.OPEN_GALLERY,
                            success = true,
                            message = "Local gallery launched"
                        )
                    )
                }
            } catch (e: Exception) {
            }

            try {
                val miuiIntent =
                    context.packageManager.getLaunchIntentForPackage("com.miui.gallery")
                if (miuiIntent != null) {
                    miuiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    context.startActivity(miuiIntent)

                    return Result.Success(
                        CameraActionResult(
                            action = CameraAction.OPEN_GALLERY,
                            success = true,
                            message = "Local gallery launched"
                        )
                    )
                }
            } catch (e: Exception) {

            }

            try {
                val oneplusIntent =
                    context.packageManager.getLaunchIntentForPackage("com.oneplus.gallery")
                if (oneplusIntent != null) {
                    oneplusIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    context.startActivity(oneplusIntent)

                    return Result.Success(
                        CameraActionResult(
                            action = CameraAction.OPEN_GALLERY,
                            success = true,
                            message = "Local gallery launched"
                        )
                    )
                }
            } catch (e: Exception) {

            }

            try {
                val mediaStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(mediaStoreIntent)

                return Result.Success(
                    CameraActionResult(
                        action = CameraAction.OPEN_GALLERY,
                        success = true,
                        message = "Local photos launched"
                    )
                )
            } catch (e: Exception) {

            }

            try {
                val imageIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(imageIntent)

                return Result.Success(
                    CameraActionResult(
                        action = CameraAction.OPEN_GALLERY,
                        success = true,
                        message = "Local image viewer launched"
                    )
                )
            } catch (e: Exception) {

            }

            try {
                val photosIntent =
                    context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
                if (photosIntent != null) {
                    photosIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    context.startActivity(photosIntent)

                    return Result.Success(
                        CameraActionResult(
                            action = CameraAction.OPEN_GALLERY,
                            success = true,
                            message = "Google Photos launched (fallback)"
                        )
                    )
                }
            } catch (e: Exception) {

            }


            Result.Error(
                AppError.CameraError.CameraOperationFailed("No gallery app could be launched")
            )

        } catch (e: Exception) {

            Result.Error(
                AppError.CameraError.CameraOperationFailed("Failed to open gallery: ${e.message}")
            )
        }
    }

    override fun getCurrentCameraFacing(): Int = currentCameraFacing

    override fun cleanup() {
        currentRecording?.stop()
        currentRecording = null
        isRecording = false
        currentCamera = null
        imageCapture = null
        videoCapture = null
        cameraExecutor.shutdown()
    }
}
