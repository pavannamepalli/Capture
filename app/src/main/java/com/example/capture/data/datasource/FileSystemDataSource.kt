package com.example.capture.data.datasource

import android.net.Uri
import com.example.capture.core.result.Result
import java.io.File

interface FileSystemDataSource {

    suspend fun createPhotoFile(): File?

    suspend fun savePhotoToGallery(photoFile: File): com.example.capture.core.result.Result<Uri>

    suspend fun createVideoFile(): File?

    suspend fun saveVideoToGallery(videoFile: File): Result<Uri>
}