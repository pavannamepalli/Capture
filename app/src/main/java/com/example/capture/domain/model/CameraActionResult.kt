
package com.example.capture.domain.model

data class CameraActionResult(
    val action: CameraAction,
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
