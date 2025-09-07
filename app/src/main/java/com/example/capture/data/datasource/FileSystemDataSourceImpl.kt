
package com.example.capture.data.datasource

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.capture.core.error.AppError
import com.example.capture.core.result.Result
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileSystemDataSourceImpl(
    private val context: Context
) : FileSystemDataSource {
    
    override suspend fun createPhotoFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "GestureCamera_$timestamp.jpg"
            
                        val picturesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "GestureCamera"
            )
            if (!picturesDir.exists()) {
                picturesDir.mkdirs()
            }
            
            File(picturesDir, fileName)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun savePhotoToGallery(photoFile: File): Result<Uri> {
        return try {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GestureCamera")
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    photoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Result.Success(uri)
            } else {
                Result.Error(AppError.FileSystemError.FileSaveFailed)
            }
        } catch (e: Exception) {
            Result.Error(
                AppError.FileSystemError.FileOperationFailed(
                    operation = "save_photo_to_gallery",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun createVideoFile(): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "GestureCamera_$timestamp.mp4"
            
                        val moviesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "GestureCamera"
            )
            if (!moviesDir.exists()) {
                moviesDir.mkdirs()
            }
            
            File(moviesDir, fileName)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun saveVideoToGallery(videoFile: File): Result<Uri> {
        return try {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/GestureCamera")
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    videoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Result.Success(uri)
            } else {
                Result.Error(AppError.FileSystemError.FileSaveFailed)
            }
        } catch (e: Exception) {
            Result.Error(
                AppError.FileSystemError.FileOperationFailed(
                    operation = "save_video_to_gallery",
                    cause = e
                )
            )
        }
    }
}
