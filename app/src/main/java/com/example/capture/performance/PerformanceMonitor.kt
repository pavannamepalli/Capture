package com.example.capture.performance

import com.example.capture.data.model.PerformanceStats
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong

class PerformanceMonitor {

    companion object {
        private const val TARGET_FPS = 30.0
        private const val MIN_FPS_THRESHOLD = 25.0
        private const val MAX_FPS_THRESHOLD = 35.0
        private const val FPS_MEASUREMENT_INTERVAL_MS = 1000L
        private const val ADAPTIVE_FRAME_SKIP_THRESHOLD = 28.0
    }

    private val _currentFPS = MutableStateFlow(0.0)

    private val _averageFPS = MutableStateFlow(0.0)

    private val _performanceStatus = MutableStateFlow(PerformanceStatus.OPTIMAL)

    private var frameCount = 0
    private var lastMeasurementTime = System.currentTimeMillis()
    private var lastFrameTime = System.currentTimeMillis()

    private val fpsHistory = mutableListOf<Double>()
    private val maxHistorySize = 10

    private var frameSkipCounter = 0
    private var adaptiveFrameSkipInterval = 1
    private var isAdaptiveProcessingEnabled = true

    private val totalFramesProcessed = AtomicLong(0)
    private val totalFramesSkipped = AtomicLong(0)
    private var startTime = System.currentTimeMillis()

    fun onFrameProcessed() {
        val currentTime = System.currentTimeMillis()
        frameCount++
        totalFramesProcessed.incrementAndGet()

        if (currentTime - lastMeasurementTime >= FPS_MEASUREMENT_INTERVAL_MS) {
            val elapsedTime = currentTime - lastMeasurementTime
            val fps = (frameCount * 1000.0) / elapsedTime

            updateFPSMetrics(fps)

            frameCount = 0
            lastMeasurementTime = currentTime
        }

        lastFrameTime = currentTime
    }

    fun onFrameSkipped() {
        totalFramesSkipped.incrementAndGet()
    }

    fun shouldProcessFrame(): Boolean {
        if (!isAdaptiveProcessingEnabled) return true

        frameSkipCounter++

        if (_currentFPS.value < ADAPTIVE_FRAME_SKIP_THRESHOLD && _currentFPS.value > 0) {
            return frameSkipCounter % adaptiveFrameSkipInterval == 0
        }

        return true
    }

    private fun updateFPSMetrics(fps: Double) {
        _currentFPS.value = fps

        fpsHistory.add(fps)
        if (fpsHistory.size > maxHistorySize) {
            fpsHistory.removeAt(0)
        }

        val averageFps = fpsHistory.average()
        _averageFPS.value = averageFps

        updatePerformanceStatus(fps)

        adjustFrameSkipInterval(fps)


        logPerformanceMetrics()
    }

    private fun updatePerformanceStatus(fps: Double) {
        _performanceStatus.value = when {
            fps >= MAX_FPS_THRESHOLD -> PerformanceStatus.OPTIMAL
            fps >= TARGET_FPS -> PerformanceStatus.GOOD
            fps >= MIN_FPS_THRESHOLD -> PerformanceStatus.ACCEPTABLE
            else -> PerformanceStatus.POOR
        }
    }

    private fun adjustFrameSkipInterval(fps: Double) {
        when {
            fps < 20.0 -> adaptiveFrameSkipInterval = 3
            fps < 25.0 -> adaptiveFrameSkipInterval = 2
            fps < 28.0 -> adaptiveFrameSkipInterval = 2
            fps >=30.0 -> adaptiveFrameSkipInterval = 1
        }
    }

    private fun logPerformanceMetrics() {
        (System.currentTimeMillis() - startTime) / 1000.0
        val totalProcessed = totalFramesProcessed.get()
        val totalSkipped = totalFramesSkipped.get()
        if (totalProcessed > 0) (totalSkipped.toDouble() / totalProcessed) * 100 else 0.0

    }

    fun getPerformanceStats(): PerformanceStats {
        val uptime = (System.currentTimeMillis() - startTime) / 1000.0
        val totalProcessed = totalFramesProcessed.get()
        val totalSkipped = totalFramesSkipped.get()
        val skipRate =
            if (totalProcessed > 0) (totalSkipped.toDouble() / totalProcessed) * 100 else 0.0

        return PerformanceStats(
            currentFPS = _currentFPS.value,
            averageFPS = _averageFPS.value,
            performanceStatus = _performanceStatus.value,
            frameSkipInterval = adaptiveFrameSkipInterval,
            totalFramesProcessed = totalProcessed,
            totalFramesSkipped = totalSkipped,
            skipRate = skipRate,
            uptimeSeconds = uptime
        )
    }

    fun setAdaptiveProcessingEnabled(enabled: Boolean) {
        isAdaptiveProcessingEnabled = enabled
        if (!enabled) {
            adaptiveFrameSkipInterval = 1
        }
    }

    fun reset() {
        frameCount = 0
        lastMeasurementTime = System.currentTimeMillis()
        lastFrameTime = System.currentTimeMillis()
        fpsHistory.clear()
        frameSkipCounter = 0
        adaptiveFrameSkipInterval = 1
        startTime = System.currentTimeMillis()
        totalFramesProcessed.set(0)
        totalFramesSkipped.set(0)

        _currentFPS.value = 0.0
        _averageFPS.value = 0.0
        _performanceStatus.value = PerformanceStatus.OPTIMAL

    }
}

