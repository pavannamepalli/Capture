package com.example.capture.architecture

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.example.capture.performance.PerformanceMonitor
import com.example.capture.data.model.PerformanceStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FrameProcessor(
    private val performanceMonitor: PerformanceMonitor
) {


    private val _frameCount = MutableStateFlow(0L)

    private val frameProcessingExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var onFrameProcessedCallback: ((Bitmap) -> Unit)? = null

    fun setOnFrameProcessedCallback(callback: (Bitmap) -> Unit) {
        onFrameProcessedCallback = callback
    }

    fun processFrame(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (!performanceMonitor.shouldProcessFrame()) {
            performanceMonitor.onFrameSkipped()
            imageProxy.close()
            return
        }

        performanceMonitor.onFrameProcessed()

        frameProcessingExecutor.execute {
            try {
                _frameCount.value++

                val processedBitmap = convertImageProxyToBitmap(imageProxy, isFrontCamera)
                onFrameProcessedCallback?.invoke(processedBitmap)


            } catch (e: Exception) {
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun convertImageProxyToBitmap(imageProxy: ImageProxy, isFrontCamera: Boolean): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }

        return Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )
    }


    fun cleanup() {
        frameProcessingExecutor.shutdown()
        onFrameProcessedCallback = null
    }
}

