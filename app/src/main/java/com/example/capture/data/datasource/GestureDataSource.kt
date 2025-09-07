
package com.example.capture.data.datasource

import android.graphics.Bitmap
import com.example.capture.core.result.Result
import com.example.capture.domain.model.GestureResult
import com.example.capture.data.model.PerformanceStats
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

interface GestureDataSource {
    
        suspend fun initialize(): Result<Unit>
    
        suspend fun processFrame(bitmap: Bitmap): Result<GestureResult>
    
        suspend fun processLandmarks(landmarkResult: HandLandmarkerResult): Result<GestureResult>
    
        fun setGestureDetectionEnabled(enabled: Boolean)
    
        fun getPerformanceStats(): PerformanceStats
    
        fun reset()
    
        fun cleanup()
}
