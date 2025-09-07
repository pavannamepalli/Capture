
package com.example.capture.domain.model

import android.net.Uri
import androidx.camera.core.CameraSelector

data class CameraState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val cameraFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val zoomRatio: Float = 1.0f,
    val isRecording: Boolean = false,
    val flashEnabled: Boolean = false,
    val lastPhotoUri: Uri? = null,
    val lastVideoUri: Uri? = null,
    val lastAction: CameraAction? = null,
    val lastActionResult: CameraActionResult? = null,
    val showGallery: Boolean = false
)
