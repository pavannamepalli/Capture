package com.example.capture.data.datasource

import android.content.Context
import android.graphics.Bitmap
import com.example.capture.core.error.AppError
import com.example.capture.core.ml.GestureDetector
import com.example.capture.core.result.Result
import com.example.capture.domain.model.GestureResult
import com.example.capture.domain.model.GestureType
import com.example.capture.performance.PerformanceMonitor
import com.example.capture.data.model.PerformanceStats
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class GestureDataSourceImpl(
) : GestureDataSource {

    private val gestureDetector = GestureDetector()
    private val performanceMonitor = PerformanceMonitor()

    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> {
        return try {
            performanceMonitor.reset()

            gestureDetector.reset()

            isInitialized = true
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                AppError.GestureError.ModelInitializationFailed
            )
        }
    }

    override suspend fun processFrame(bitmap: Bitmap): Result<GestureResult> {
        return try {
            if (!isInitialized) {
                return Result.Error(
                    AppError.GestureError.ModelNotLoaded
                )
            }

            if (!performanceMonitor.shouldProcessFrame()) {
                performanceMonitor.onFrameSkipped()
                return Result.Success(GestureResult(GestureType.NONE, 0f))
            }

            performanceMonitor.onFrameProcessed()


            val currentGesture = gestureDetector.gestureResult.value
            val domainGesture = convertToDomainGesture(currentGesture)
            Result.Success(domainGesture)

        } catch (e: Exception) {
            Result.Error(
                AppError.GestureError.GestureDetectionFailed(e)
            )
        }
    }

    override suspend fun processLandmarks(landmarkResult: HandLandmarkerResult): Result<GestureResult> {
        return try {
            if (!isInitialized) {
                return Result.Error(
                    AppError.GestureError.ModelNotLoaded
                )
            }

            if (!performanceMonitor.shouldProcessFrame()) {
                performanceMonitor.onFrameSkipped()
                return Result.Success(GestureResult(GestureType.NONE, 0f))
            }

            performanceMonitor.onFrameProcessed()

            gestureDetector.processLandmarks(landmarkResult)

            val currentGesture = gestureDetector.gestureResult.value
            val domainGesture = convertToDomainGesture(currentGesture)
            Result.Success(domainGesture)

        } catch (e: Exception) {
            Result.Error(
                AppError.GestureError.GestureDetectionFailed(e)
            )
        }
    }

    override fun setGestureDetectionEnabled(enabled: Boolean) {
        gestureDetector.setAdaptiveProcessingEnabled(enabled)
    }

    override fun getPerformanceStats(): PerformanceStats {
        return performanceMonitor.getPerformanceStats()
    }

    override fun reset() {
        gestureDetector.reset()
        performanceMonitor.reset()
    }

    override fun cleanup() {
        gestureDetector.reset()
        performanceMonitor.reset()
        isInitialized = false
    }

    private fun convertToDomainGesture(oldGesture: com.example.capture.core.ml.GestureResult): GestureResult {
        return GestureResult(
            gestureType = GestureType.valueOf(oldGesture.gestureType.name),
            confidence = oldGesture.confidence
        )
    }
}
