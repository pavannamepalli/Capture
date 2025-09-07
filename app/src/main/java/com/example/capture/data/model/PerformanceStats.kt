package com.example.capture.data.model

import com.example.capture.performance.PerformanceStatus

data class PerformanceStats(
    val currentFPS: Double,
    val averageFPS: Double,
    val performanceStatus: PerformanceStatus,
    val frameSkipInterval: Int,
    val totalFramesProcessed: Long,
    val totalFramesSkipped: Long,
    val skipRate: Double,
    val uptimeSeconds: Double
)
