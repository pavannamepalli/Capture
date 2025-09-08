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
import androidx.camera.core.CameraSelector
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
        
        val currentMessage = _uiState.value.cooldownMessage
        if (currentMessage == context.getString(R.string.cooldown_try_other_gesture) ||
            currentMessage == context.getString(R.string.action_can_stop_video)) {
            _uiState.value = _uiState.value.copy(
                showCooldown = false, cooldownMessage = null
            )
        }

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
                val isFrontCamera = cameraRepository.cameraState.value.cameraFacing == CameraSelector.LENS_FACING_FRONT
                if (isFrontCamera) {
                    return Result.Success(
                        CameraActionResult(
                            action = CameraAction.TOGGLE_FLASH,
                            success = true,
                            message = context.getString(R.string.action_flash_front_camera)
                        )
                    )
                }
                
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


    private fun startMessageTimingSequence(action: CameraAction) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val cooldownDuration = when (action) {
                    CameraAction.CAPTURE_PHOTO -> universalGestureCooldownMs
                    CameraAction.START_VIDEO_RECORDING -> videoStartCooldownMs
                    CameraAction.STOP_VIDEO_RECORDING -> videoStopCooldownMs
                    CameraAction.TOGGLE_FLASH -> flashToggleCooldownMs
                    CameraAction.SWITCH_CAMERA -> cameraSwitchCooldownMs
                    CameraAction.ZOOM_IN -> universalGestureCooldownMs
                    CameraAction.ZOOM_OUT -> universalGestureCooldownMs
                    CameraAction.OPEN_GALLERY -> universalGestureCooldownMs
                    else -> universalGestureCooldownMs
                }

                val startTime = System.currentTimeMillis()
                
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = (cooldownDuration - elapsed) / 1000
                    
                    if (remaining <= 0) {
                        when (action) {
                            CameraAction.START_VIDEO_RECORDING -> {
                                _uiState.value = _uiState.value.copy(
                                    cooldownMessage = context.getString(R.string.action_can_stop_video),
                                    showCooldown = true
                                )
                            }
                            else -> {
                                updateTryOtherGestureUI()
                            }
                        }
                        break
                    } else {
                        val message = when (action) {
                            CameraAction.CAPTURE_PHOTO -> context.getString(R.string.cooldown_try_gesture_in, remaining)
                            CameraAction.START_VIDEO_RECORDING -> context.getString(R.string.cooldown_video_period, remaining)
                            CameraAction.STOP_VIDEO_RECORDING -> context.getString(R.string.cooldown_video_start_period, remaining)
                            CameraAction.TOGGLE_FLASH -> context.getString(R.string.cooldown_flash_toggle_remaining, remaining)
                            CameraAction.SWITCH_CAMERA -> context.getString(R.string.cooldown_camera_switch_remaining, remaining)
                            CameraAction.ZOOM_IN -> context.getString(R.string.cooldown_try_gesture_in, remaining)
                            CameraAction.ZOOM_OUT -> context.getString(R.string.cooldown_try_gesture_in, remaining)
                            CameraAction.OPEN_GALLERY -> context.getString(R.string.cooldown_try_gesture_in, remaining)
                            else -> context.getString(R.string.cooldown_try_gesture_in, remaining)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            cooldownMessage = message, showCooldown = true
                        )
                    }
                    
                    delay(1000L)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun updateRecordingBlockUI() {
        _uiState.value = _uiState.value.copy(
            cooldownMessage = context.getString(R.string.action_stop_video_first),
            showCooldown = true
        )
    }

   }

