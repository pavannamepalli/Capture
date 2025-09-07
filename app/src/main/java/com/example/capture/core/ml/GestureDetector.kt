package com.example.capture.core.ml

import com.example.capture.performance.PerformanceMonitor
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GestureDetector {

    private val _gestureResult = MutableStateFlow(GestureResult(GestureType.NONE, 0f))
    val gestureResult: StateFlow<GestureResult> = _gestureResult.asStateFlow()

    private val performanceMonitor = PerformanceMonitor()

    private val minConfidence = 0.2f
    private val gestureStabilityFrames = 1
    private val recentGestures = mutableListOf<GestureType>()

    private var lastGestureTime = 0L
    private val gestureCooldownMs = 100L
    private var previousPinchDistance = -1f
    private var pinchGestureStartTime = 0L
    private var lastPinchZoomTime = 0L
    private val pinchGestureMinDuration = 200L
    private val pinchDistanceThreshold = 0.01f
    private val pinchZoomCooldownMs = 500L
    fun processLandmarks(result: HandLandmarkerResult) {
        if (!performanceMonitor.shouldProcessFrame()) {
            performanceMonitor.onFrameSkipped()
            return
        }

        performanceMonitor.onFrameProcessed()

        if (result.landmarks().isEmpty()) {
            updateGesture(GestureType.NONE, 0f)
            return
        }

        val landmarks = result.landmarks()[0]

        if (!isCompleteHandVisible(landmarks)) {
            updateGesture(GestureType.NONE, 0f)
            return
        }

        if (!isHandInsideGestureBox(landmarks)) {
            updateGesture(GestureType.NONE, 0f)
            return
        }

        val detectedGesture = detectGesture(landmarks)

        recentGestures.add(detectedGesture.gestureType)
        if (recentGestures.size > gestureStabilityFrames) {
            recentGestures.removeAt(0)
        }

        val currentTime = System.currentTimeMillis()
        if (isGestureStable(detectedGesture.gestureType) && (currentTime - lastGestureTime) >= gestureCooldownMs) {
            updateGesture(detectedGesture.gestureType, detectedGesture.confidence)
            lastGestureTime = currentTime
        }
    }

    private fun detectGesture(landmarks: List<NormalizedLandmark>): GestureResult {
        val openPalmConfidence = detectOpenPalm(landmarks)
        val peaceSignConfidence = detectPeaceSign(landmarks)
        val thumbsUpConfidence = detectThumbsUp(landmarks)
        val okSignConfidence = detectOkSign(landmarks)
        val pinchZoomResult = detectPinchZoom(landmarks)
        val threeFingersUpConfidence = detectThreeFingersUp(landmarks)



        logFingerStates(landmarks)

        if (pinchZoomResult.confidence > 0.5f) {
            return pinchZoomResult
        }

        val gestures = listOf(
            GestureResult(GestureType.OPEN_PALM, openPalmConfidence),
            GestureResult(GestureType.PEACE_SIGN, peaceSignConfidence),
            GestureResult(GestureType.THUMBS_UP, thumbsUpConfidence),
            GestureResult(GestureType.OK_SIGN, okSignConfidence),
            GestureResult(GestureType.THREE_FINGERS_UP, threeFingersUpConfidence)
        )

        val bestGesture = gestures.maxByOrNull { it.confidence }


        return bestGesture ?: GestureResult(GestureType.NONE, 0f)
    }

    private fun detectOpenPalm(landmarks: List<NormalizedLandmark>): Float {
        val extendedFingers = HandLandmarkUtils.getExtendedFingerCount(landmarks)
        val thumbExtended = HandLandmarkUtils.isThumbExtended(landmarks)


        return if (extendedFingers == 4 && thumbExtended) 0.9f else 0f
    }

    private fun detectPeaceSign(landmarks: List<NormalizedLandmark>): Float {
        val indexExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )
        val middleExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        val ringExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        val pinkyExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )
        HandLandmarkUtils.isThumbExtended(landmarks)

        val isPeaceSign = indexExtended && middleExtended && !ringExtended && !pinkyExtended

        HandLandmarkUtils.calculateDistance(
            landmarks[HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP],
            landmarks[HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP]
        )

        if (isPeaceSign) {

            return 0.9f
        }

        val indexTip = landmarks[HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP]
        val middleTip = landmarks[HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP]
        val ringTip = landmarks[HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP]
        val pinkyTip = landmarks[HandLandmarkUtils.LandmarkIndices.PINKY_TIP]
        val thumbTip = landmarks[HandLandmarkUtils.LandmarkIndices.THUMB_TIP]

        val indexHigherThanRing = indexTip.y() < ringTip.y()
        val indexHigherThanPinky = indexTip.y() < pinkyTip.y()
        val middleHigherThanRing = middleTip.y() < ringTip.y()
        val middleHigherThanPinky = middleTip.y() < pinkyTip.y()
        indexTip.y() < thumbTip.y()
        middleTip.y() < thumbTip.y()

        val alternativePeaceSign =
            isPeaceSign && indexHigherThanRing && indexHigherThanPinky && middleHigherThanRing && middleHigherThanPinky

        if (alternativePeaceSign) {

            return 0.8f
        }

        return 0f
    }

    private fun detectThumbsUp(landmarks: List<NormalizedLandmark>): Float {
        val thumbExtended = HandLandmarkUtils.isThumbExtended(landmarks)

        val indexExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )
        val middleExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        val ringExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        val pinkyExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )

        val isThumbsUp =
            thumbExtended && !indexExtended && !middleExtended && !ringExtended && !pinkyExtended



        return if (isThumbsUp) 0.9f else 0f
    }

    private fun detectOkSign(landmarks: List<NormalizedLandmark>): Float {
        val thumbExtended = HandLandmarkUtils.isThumbExtended(landmarks)
        val indexExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )

        val middleExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        val ringExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        val pinkyExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )

        val thumbIndexClose = HandLandmarkUtils.areFingertipsClose(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.THUMB_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            0.15f
        )

        val isOkSign =
            thumbExtended && !indexExtended && middleExtended && ringExtended && pinkyExtended && thumbIndexClose
        val indexTip = landmarks[HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP]
        val indexPip = landmarks[HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP]
        val properCircle = indexTip.y() >= (indexPip.y() - 0.08f)


        return if (isOkSign && properCircle) {
            0.9f
        } else 0f
    }

    private fun detectPinchZoom(landmarks: List<NormalizedLandmark>): GestureResult {
        val thumbExtended = HandLandmarkUtils.isThumbExtended(landmarks)
        val indexExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )

        val middleExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        val ringExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        val pinkyExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )

        val isPinchGesture =
            thumbExtended && indexExtended && !middleExtended && !ringExtended && !pinkyExtended

        val thumbTip = landmarks[HandLandmarkUtils.LandmarkIndices.THUMB_TIP]
        val indexTip = landmarks[HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP]
        val thumbIndexDistance = HandLandmarkUtils.calculateDistance(thumbTip, indexTip)

        val isNotOkSign = thumbIndexDistance > 0.1f
        val isValidPinchGesture = isPinchGesture && isNotOkSign


        if (!isValidPinchGesture) {
            previousPinchDistance = -1f
            pinchGestureStartTime = 0L
            return GestureResult(GestureType.NONE, 0f)
        }

        val currentDistance = thumbIndexDistance

        val currentTime = System.currentTimeMillis()

        if (previousPinchDistance < 0) {
            previousPinchDistance = currentDistance
            pinchGestureStartTime = currentTime
            return GestureResult(GestureType.NONE, 0f)
        }

        if (currentTime - pinchGestureStartTime < pinchGestureMinDuration) {
            return GestureResult(GestureType.NONE, 0f)
        }

        if (currentTime - lastPinchZoomTime < pinchZoomCooldownMs) {
            return GestureResult(GestureType.NONE, 0f)
        }

        val distanceChange = currentDistance - previousPinchDistance



        return when {
            distanceChange > pinchDistanceThreshold -> {
                lastPinchZoomTime = currentTime

                GestureResult(GestureType.PINCH_ZOOM_IN, 0.9f)
            }

            distanceChange < -pinchDistanceThreshold -> {
                lastPinchZoomTime = currentTime

                GestureResult(GestureType.PINCH_ZOOM_OUT, 0.9f)
            }

            else -> {

                GestureResult(GestureType.NONE, 0f)
            }
        }
    }

    private fun detectThreeFingersUp(landmarks: List<NormalizedLandmark>): Float {
        val indexExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )
        val thumbExtended = HandLandmarkUtils.isThumbExtended(landmarks)
        val middleExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        val ringExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        val pinkyExtended = HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )

        val isThreeFingersUp =
            indexExtended && thumbExtended && !middleExtended && !ringExtended && pinkyExtended



        return if (isThreeFingersUp) {
            0.9f
        } else 0f
    }

    private fun logFingerStates(landmarks: List<NormalizedLandmark>) {
        HandLandmarkUtils.isThumbExtended(landmarks)
        HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_PIP
        )
        HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_PIP
        )
        HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_PIP
        )
        HandLandmarkUtils.isFingerExtended(
            landmarks,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_PIP
        )


    }

    private fun isCompleteHandVisible(landmarks: List<NormalizedLandmark>): Boolean {
        if (landmarks.size < 21) {
            return false
        }

        val allInBounds = landmarks.all { landmark ->
            landmark.x() >= 0.0f && landmark.x() <= 1.0f && landmark.y() >= 0.0f && landmark.y() <= 1.0f
        }

        val notAtEdges = landmarks.all { landmark ->
            landmark.x() >= 0.05f && landmark.x() <= 0.95f && landmark.y() >= 0.05f && landmark.y() <= 0.95f
        }

        val wrist = landmarks[HandLandmarkUtils.LandmarkIndices.WRIST]
        val middleTip = landmarks[HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP]
        val handSize = HandLandmarkUtils.calculateDistance(wrist, middleTip)
        val reasonableSize = handSize > 0.03f && handSize < 0.6f
        return allInBounds && notAtEdges && reasonableSize
    }

    private fun isHandInsideGestureBox(landmarks: List<NormalizedLandmark>): Boolean {
        if (landmarks.isEmpty()) return false

        val boxLeft = 0.15f
        val boxRight = 0.85f
        val boxTop = 0.175f
        val boxBottom = 0.825f
        val keyLandmarks = listOf(
            HandLandmarkUtils.LandmarkIndices.WRIST,
            HandLandmarkUtils.LandmarkIndices.THUMB_TIP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_TIP,
            HandLandmarkUtils.LandmarkIndices.PINKY_TIP,
            HandLandmarkUtils.LandmarkIndices.THUMB_MCP,
            HandLandmarkUtils.LandmarkIndices.INDEX_FINGER_MCP,
            HandLandmarkUtils.LandmarkIndices.MIDDLE_FINGER_MCP,
            HandLandmarkUtils.LandmarkIndices.RING_FINGER_MCP,
            HandLandmarkUtils.LandmarkIndices.PINKY_MCP
        )

        val allInside = keyLandmarks.all { index ->
            if (index < landmarks.size) {
                val landmark = landmarks[index]
                val x = landmark.x()
                val y = landmark.y()
                val isInside = x >= boxLeft && x <= boxRight && y >= boxTop && y <= boxBottom


                isInside
            } else {
                false
            }
        }



        return allInside
    }

    private fun isGestureStable(gestureType: GestureType): Boolean {
        if (recentGestures.size < gestureStabilityFrames) return false

        return recentGestures.all { it == gestureType }
    }

    private fun updateGesture(gestureType: GestureType, confidence: Float) {
        if (confidence >= minConfidence) {
            _gestureResult.value = GestureResult(gestureType, confidence)
        } else {
            _gestureResult.value = GestureResult(GestureType.NONE, 0f)
        }
    }

    fun reset() {
        recentGestures.clear()
        _gestureResult.value = GestureResult(GestureType.NONE, 0f)
        performanceMonitor.reset()
    }


    fun setAdaptiveProcessingEnabled(enabled: Boolean) {
        performanceMonitor.setAdaptiveProcessingEnabled(enabled)
    }
}
