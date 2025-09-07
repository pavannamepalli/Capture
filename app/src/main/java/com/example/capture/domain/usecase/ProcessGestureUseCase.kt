package com.example.capture.domain.usecase

import android.content.Context
import com.example.capture.R
import com.example.capture.core.error.AppError
import com.example.capture.core.result.Result
import com.example.capture.data.repository.CameraRepository
import com.example.capture.data.repository.GestureRepository
import com.example.capture.domain.model.CameraAction
import com.example.capture.domain.model.CameraActionResult
import com.example.capture.domain.model.GestureResult
import com.example.capture.domain.model.GestureType
import com.example.capture.presentation.ui.GestureProcessingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProcessGestureUseCase(
    private val cameraRepository: CameraRepository,
    private val context: Context
) {

    private var lastGestureTime = -1L
    private var lastVideoStartTime = 0L
    private var lastVideoStopTime = 0L
    private var lastFlashToggleTime = -1L
    private var lastCameraSwitchTime = -1L
    private val universalGestureCooldownMs = 3000L
    private val videoStartCooldownMs = 1000L
    private val videoStopCooldownMs = 2000L
    private val flashToggleCooldownMs = 2000L
    private val cameraSwitchCooldownMs = 3000L


    private val _uiState = MutableStateFlow(GestureProcessingUiState())
    val uiState: StateFlow<GestureProcessingUiState> = _uiState.asStateFlow()

    suspend fun processGesture(gesture: GestureResult): Result<CameraActionResult> {
        val currentTime = System.currentTimeMillis()

        if (lastGestureTime != -1L) {
            val timeSinceLastGesture = currentTime - lastGestureTime
            if (timeSinceLastGesture < universalGestureCooldownMs) {
                val remainingTime = (universalGestureCooldownMs - timeSinceLastGesture) / 1000
                updateUniversalGestureCooldownUI(remainingTime)
                return Result.Success(
                    CameraActionResult(
                        action = CameraAction.CAPTURE_PHOTO,
                        success = false,
                        message = context.getString(R.string.cooldown_try_gesture_in, remainingTime)
                    )
                )
            }
        }

        val isRecording = cameraRepository.cameraState.value.isRecording


        if (isRecording) {
            if (gesture.gestureType == GestureType.PEACE_SIGN) {
                val timeSinceVideoStart = currentTime - lastVideoStartTime
                if (timeSinceVideoStart < videoStartCooldownMs) {
                    val remainingTime = (videoStartCooldownMs - timeSinceVideoStart) / 1000
                    updateVideoCooldownUI(remainingTime)
                    return Result.Error(
                        AppError.GenericError.BusinessLogicError(
                            context.getString(R.string.cooldown_video_period, remainingTime)
                        )
                    )
                }
            } else {
                updateRecordingBlockUI()
                return Result.Error(
                    AppError.GenericError.BusinessLogicError(
                        context.getString(R.string.cooldown_gestures_blocked_recording)
                    )
                )
            }
        }


        val action = when (gesture.gestureType) {
            GestureType.OPEN_PALM -> CameraAction.CAPTURE_PHOTO
            GestureType.PEACE_SIGN -> {
                if (isRecording) {
                    CameraAction.STOP_VIDEO_RECORDING
                } else {
                    val timeSinceVideoStop = currentTime - lastVideoStopTime
                    if (timeSinceVideoStop < videoStopCooldownMs) {
                        val remainingTime = (videoStopCooldownMs - timeSinceVideoStop) / 1000
                        updateVideoStopCooldownUI(remainingTime)
                        return Result.Error(
                            AppError.GenericError.BusinessLogicError(
                                context.getString(
                                    R.string.cooldown_video_start_period,
                                    remainingTime
                                )
                            )
                        )
                    } else {
                        CameraAction.START_VIDEO_RECORDING
                    }
                }
            }

            GestureType.THUMBS_UP -> {
                if (lastCameraSwitchTime == -1L) {
                } else {
                    val timeSinceCameraSwitch = currentTime - lastCameraSwitchTime
                    if (timeSinceCameraSwitch < cameraSwitchCooldownMs) {
                        val remainingTime = (cameraSwitchCooldownMs - timeSinceCameraSwitch) / 1000
                        updateCameraSwitchCooldownUI(remainingTime)
                        return Result.Success(
                            CameraActionResult(
                                action = CameraAction.SWITCH_CAMERA,
                                success = false,
                                message = context.getString(
                                    R.string.cooldown_camera_switch_remaining,
                                    remainingTime
                                )
                            )
                        )
                    }
                }
                CameraAction.SWITCH_CAMERA
            }

            GestureType.OK_SIGN -> {
                CameraAction.OPEN_GALLERY
            }

            GestureType.PINCH_ZOOM_IN -> {
                CameraAction.ZOOM_IN
            }

            GestureType.PINCH_ZOOM_OUT -> {
                CameraAction.ZOOM_OUT
            }

            GestureType.THREE_FINGERS_UP -> {
                if (lastFlashToggleTime == -1L) {
                } else {
                    val timeSinceFlashToggle = currentTime - lastFlashToggleTime
                    if (timeSinceFlashToggle < flashToggleCooldownMs) {
                        val remainingTime = (flashToggleCooldownMs - timeSinceFlashToggle) / 1000
                        updateFlashToggleCooldownUI(remainingTime)
                        return Result.Success(
                            CameraActionResult(
                                action = CameraAction.TOGGLE_FLASH,
                                success = false,
                                message = context.getString(
                                    R.string.cooldown_flash_toggle_remaining,
                                    remainingTime
                                )
                            )
                        )
                    }
                }
                CameraAction.TOGGLE_FLASH
            }

            GestureType.NONE -> {
                return Result.Success(
                    CameraActionResult(
                        action = CameraAction.CAPTURE_PHOTO,
                        success = true,
                        message = context.getString(R.string.action_no_gesture_detected)
                    )
                )
            }
        }

        val result = cameraRepository.executeCameraAction(action)

        if (!result.isSuccess) {
            val errorMessage = result.getOrNull()?.message ?: "Action failed"
            _uiState.value = _uiState.value.copy(
                cooldownMessage = errorMessage, showCooldown = true
            )
        }

        if (result.isSuccess) {
            lastGestureTime = currentTime

            showActionFeedbackMessage(action)

            if (action == CameraAction.START_VIDEO_RECORDING) {
                lastVideoStartTime = currentTime
            }

            if (action == CameraAction.STOP_VIDEO_RECORDING) {
                lastVideoStopTime = currentTime
            }

            if (action == CameraAction.TOGGLE_FLASH) {
                lastFlashToggleTime = currentTime
            }

            if (action == CameraAction.SWITCH_CAMERA) {
                lastCameraSwitchTime = currentTime
            }

            startMessageTimingSequence(action)
        }

        return result
    }


    private fun updateVideoCooldownUI(remainingSeconds: Long) {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(R.string.cooldown_video_period, remainingSeconds),
            showCooldown = true
        )
    }

    private fun updateVideoStopCooldownUI(remainingSeconds: Long) {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(
                R.string.cooldown_video_start_period,
                remainingSeconds
            ), showCooldown = true
        )
    }

    private fun updateFlashToggleCooldownUI(remainingSeconds: Long) {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(
                R.string.cooldown_flash_toggle_remaining,
                remainingSeconds
            ), showCooldown = true
        )
    }

    private fun updateCameraSwitchCooldownUI(remainingSeconds: Long) {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(
                R.string.cooldown_camera_switch_remaining,
                remainingSeconds
            ), showCooldown = true
        )
    }

    private fun updateUniversalGestureCooldownUI(remainingSeconds: Long) {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(R.string.cooldown_try_gesture_in, remainingSeconds),
            showCooldown = true
        )
    }

    private fun updateTryOtherGestureUI() {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(R.string.cooldown_try_other_gesture),
            showCooldown = true
        )
    }

    private fun showActionFeedbackMessage(action: CameraAction) {
        val message = when (action) {
            CameraAction.CAPTURE_PHOTO -> context.getString(R.string.action_photo_captured)
            CameraAction.START_VIDEO_RECORDING -> context.getString(R.string.action_video_recording_started)
            CameraAction.STOP_VIDEO_RECORDING -> context.getString(R.string.action_video_recording_stopped)
            CameraAction.TOGGLE_FLASH -> context.getString(R.string.action_flash_toggled)
            CameraAction.SWITCH_CAMERA -> context.getString(R.string.action_camera_switched)
            CameraAction.ZOOM_IN -> context.getString(R.string.action_zoomed_in)
            CameraAction.ZOOM_OUT -> context.getString(R.string.action_zoomed_out)
            CameraAction.OPEN_GALLERY -> context.getString(R.string.action_gallery_opened)
        }

        _uiState.value = _uiState.value.copy(
            cooldownMessage = message, showCooldown = true
        )

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            delay(2000L)
            _uiState.value = _uiState.value.copy(
                showCooldown = false, cooldownMessage = null
            )
        }
    }

    private fun startMessageTimingSequence(action: CameraAction) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                delay(2000L)

                if (action != CameraAction.START_VIDEO_RECORDING) {
                    updateTryOtherGestureUI()

                    delay(1000L)
                    _uiState.value = _uiState.value.copy(
                        showCooldown = false, cooldownMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        showCooldown = false, cooldownMessage = null
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun updateRecordingBlockUI() {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(R.string.cooldown_gestures_blocked_recording),
            showCooldown = true
        )
    }

   }

