package com.example.capture.core.ml


import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

object HandLandmarkUtils {

    object LandmarkIndices {
        const val WRIST = 0

        const val THUMB_MCP = 2
        const val THUMB_TIP = 4

        const val INDEX_FINGER_MCP = 5
        const val INDEX_FINGER_PIP = 6
        const val INDEX_FINGER_TIP = 8

        const val MIDDLE_FINGER_MCP = 9
        const val MIDDLE_FINGER_PIP = 10
        const val MIDDLE_FINGER_TIP = 12

        const val RING_FINGER_MCP = 13
        const val RING_FINGER_PIP = 14
        const val RING_FINGER_TIP = 16

        const val PINKY_MCP = 17
        const val PINKY_PIP = 18
        const val PINKY_TIP = 20
    }

    fun calculateDistance(
        landmark1: NormalizedLandmark, landmark2: NormalizedLandmark
    ): Float {
        val dx = landmark1.x() - landmark2.x()
        val dy = landmark1.y() - landmark2.y()
        val dz = landmark1.z() - landmark2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun isFingerExtended(
        landmarks: List<NormalizedLandmark>, tipIndex: Int, pipIndex: Int
    ): Boolean {
        val threshold = 0.005f
        val tipY = landmarks[tipIndex].y()
        val pipY = landmarks[pipIndex].y()
        val isExtended = tipY < (pipY - threshold)

        return isExtended
    }

    fun isThumbExtended(
        landmarks: List<NormalizedLandmark>
    ): Boolean {
        val thumbTip = landmarks[LandmarkIndices.THUMB_TIP]
        val thumbMCP = landmarks[LandmarkIndices.THUMB_MCP]
        val wrist = landmarks[LandmarkIndices.WRIST]

        val tipDistance = calculateDistance(thumbTip, wrist)
        val mcpDistance = calculateDistance(thumbMCP, wrist)

        val threshold = 0.01f
        val isExtended = tipDistance > (mcpDistance + threshold)


        return isExtended
    }

    fun areFingertipsClose(
        landmarks: List<NormalizedLandmark>,
        tip1Index: Int,
        tip2Index: Int,
        threshold: Float = 0.05f
    ): Boolean {
        return calculateDistance(landmarks[tip1Index], landmarks[tip2Index]) < threshold
    }

    fun getExtendedFingerCount(
        landmarks: List<NormalizedLandmark>
    ): Int {
        var count = 0

        if (isFingerExtended(
                landmarks,
                LandmarkIndices.INDEX_FINGER_TIP,
                LandmarkIndices.INDEX_FINGER_PIP
            )
        ) {
            count++
        }

        if (isFingerExtended(
                landmarks,
                LandmarkIndices.MIDDLE_FINGER_TIP,
                LandmarkIndices.MIDDLE_FINGER_PIP
            )
        ) {
            count++
        }

        if (isFingerExtended(
                landmarks,
                LandmarkIndices.RING_FINGER_TIP,
                LandmarkIndices.RING_FINGER_PIP
            )
        ) {
            count++
        }

        if (isFingerExtended(landmarks, LandmarkIndices.PINKY_TIP, LandmarkIndices.PINKY_PIP)) {
            count++
        }

        return count
    }
}
