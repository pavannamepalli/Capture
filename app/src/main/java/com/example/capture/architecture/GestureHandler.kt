package com.example.capture.architecture

import android.content.Context
import android.graphics.Bitmap
import com.example.capture.core.ml.GestureDetector
import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.GestureType
import com.example.capture.core.ml.HandLandmarkerHelper
import com.example.capture.performance.PerformanceMonitor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureHandler(
    private val context: Context, private val performanceMonitor: PerformanceMonitor
) {

    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    private val gestureDetector = GestureDetector()

    private val _currentGesture = MutableStateFlow(GestureResult(GestureType.NONE, 0f))

    private val _isDetecting = MutableStateFlow(false)

    private val _detectionCount = MutableStateFlow(0L)

    private val mlProcessingExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var onGestureDetectedCallback: ((GestureResult) -> Unit)? = null

    fun initialize() {
        try {
            mlProcessingExecutor.execute {
                handLandmarkerHelper = HandLandmarkerHelper(
                    context = context,
                    runningMode = RunningMode.IMAGE,
                    minHandDetectionConfidence = 0.5f,
                    minHandTrackingConfidence = 0.5f,
                    minHandPresenceConfidence = 0.5f,
                    maxNumHands = 1,
                    currentDelegate = HandLandmarkerHelper.DELEGATE_CPU,
                    handLandmarkerHelperListener = object :
                        HandLandmarkerHelper.LandmarkerListener {
                        override fun onError(error: String, errorCode: Int) {
                        }

                        override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                            if (resultBundle.results.isNotEmpty()) {
                                processHandLandmarks(resultBundle.results.first())
                            }
                        }
                    })
            }
        } catch (_: Exception) {
        }
    }

    fun setOnGestureDetectedCallback(callback: (GestureResult) -> Unit) {
        onGestureDetectedCallback = callback
    }

    fun processFrame(bitmap: Bitmap) {
        if (handLandmarkerHelper == null) {
            return
        }

        if (!performanceMonitor.shouldProcessFrame()) {
            performanceMonitor.onFrameSkipped()
            return
        }

        mlProcessingExecutor.execute {
            try {
                _isDetecting.value = true
                _detectionCount.value++

                BitmapImageBuilder(bitmap).build()

                val result = handLandmarkerHelper?.detectImage(bitmap)

                if (result != null && result.results.isNotEmpty()) {
                    processHandLandmarks(result.results.first())
                } else {
                    updateGesture(GestureResult(GestureType.NONE, 0f))
                }


            } catch (e: Exception) {
            } finally {
                _isDetecting.value = false
            }
        }
    }

    private fun processHandLandmarks(landmarkResult: com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult) {
        try {
            gestureDetector.processLandmarks(landmarkResult)

            val detectedGesture = gestureDetector.gestureResult.value

            updateGesture(detectedGesture)

            onGestureDetectedCallback?.invoke(detectedGesture)

        } catch (e: Exception) {
        }
    }

    private fun updateGesture(gesture: GestureResult) {
        _currentGesture.value = gesture
    }


    fun cleanup() {
        handLandmarkerHelper?.clearHandLandmarker()
        handLandmarkerHelper = null
        mlProcessingExecutor.shutdown()
        onGestureDetectedCallback = null
    }
}

