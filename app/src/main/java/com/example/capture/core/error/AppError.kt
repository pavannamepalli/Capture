
package com.example.capture.core.error

import android.content.Context
import com.example.capture.R

sealed class AppError : Exception() {
    
        sealed class NetworkError : AppError() {
        object NoInternetConnection : NetworkError()
        object Timeout : NetworkError()
        object ServerError : NetworkError()
        data class HttpError(val code: Int, override val message: String) : NetworkError()
    }
    
        sealed class CameraError : AppError() {
        object PermissionDenied : CameraError()
        object CameraNotAvailable : CameraError()
        object CameraInitializationFailed : CameraError()
        object CameraCaptureFailed : CameraError()
        object CameraRecordingFailed : CameraError()
        data class CameraOperationFailed(val operation: String, override val cause: Throwable? = null) : CameraError()
    }
    
        sealed class GestureError : AppError() {
        object ModelNotLoaded : GestureError()
        object ModelInitializationFailed : GestureError()
        object HandNotDetected : GestureError()
        object InvalidHandLandmarks : GestureError()
        data class GestureDetectionFailed(override val cause: Throwable? = null) : GestureError()
    }
    
        sealed class FileSystemError : AppError() {
        object StoragePermissionDenied : FileSystemError()
        object InsufficientStorage : FileSystemError()
        object FileNotFound : FileSystemError()
        object FileCreationFailed : FileSystemError()
        object FileSaveFailed : FileSystemError()
        data class FileOperationFailed(val operation: String, override val cause: Throwable? = null) : FileSystemError()
    }
    
        sealed class GenericError : AppError() {
        object UnknownError : GenericError()
        data class ValidationError(override val message: String) : GenericError()
        data class BusinessLogicError(override val message: String) : GenericError()
        data class UnexpectedError(override val cause: Throwable) : GenericError()
    }
}

fun AppError.getUserMessage(context: Context): String {
    return when (this) {
        is AppError.NetworkError.NoInternetConnection -> context.getString(R.string.error_no_internet_connection)
        is AppError.NetworkError.Timeout -> context.getString(R.string.error_request_timeout)
        is AppError.NetworkError.ServerError -> context.getString(R.string.error_server_error)
        is AppError.NetworkError.HttpError -> context.getString(R.string.error_network_error, message)
        
        is AppError.CameraError.PermissionDenied -> context.getString(R.string.error_camera_permission_denied)
        is AppError.CameraError.CameraNotAvailable -> context.getString(R.string.error_camera_not_available)
        is AppError.CameraError.CameraInitializationFailed -> context.getString(R.string.error_camera_initialization_failed)
        is AppError.CameraError.CameraCaptureFailed -> context.getString(R.string.error_camera_capture_failed)
        is AppError.CameraError.CameraRecordingFailed -> context.getString(R.string.error_camera_recording_failed)
        is AppError.CameraError.CameraOperationFailed -> context.getString(R.string.error_camera_operation_failed, operation)
        
        is AppError.GestureError.ModelNotLoaded -> context.getString(R.string.error_gesture_model_not_loaded)
        is AppError.GestureError.ModelInitializationFailed -> context.getString(R.string.error_gesture_model_initialization_failed)
        is AppError.GestureError.HandNotDetected -> context.getString(R.string.error_hand_not_detected)
        is AppError.GestureError.InvalidHandLandmarks -> context.getString(R.string.error_invalid_hand_landmarks)
        is AppError.GestureError.GestureDetectionFailed -> context.getString(R.string.error_gesture_detection_failed)
        
        is AppError.FileSystemError.StoragePermissionDenied -> context.getString(R.string.error_storage_permission_denied)
        is AppError.FileSystemError.InsufficientStorage -> context.getString(R.string.error_insufficient_storage)
        is AppError.FileSystemError.FileNotFound -> context.getString(R.string.error_file_not_found)
        is AppError.FileSystemError.FileCreationFailed -> context.getString(R.string.error_file_creation_failed)
        is AppError.FileSystemError.FileSaveFailed -> context.getString(R.string.error_file_save_failed)
        is AppError.FileSystemError.FileOperationFailed -> context.getString(R.string.error_file_operation_failed, operation)
        
        is AppError.GenericError.UnknownError -> context.getString(R.string.error_unknown_error)
        is AppError.GenericError.ValidationError -> message
        is AppError.GenericError.BusinessLogicError -> message
        is AppError.GenericError.UnexpectedError -> context.getString(R.string.error_unexpected_error, cause.message ?: "Unknown")
    }
}