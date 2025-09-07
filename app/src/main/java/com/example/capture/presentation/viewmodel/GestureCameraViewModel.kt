package com.example.capture.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.core.error.AppError
import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.GestureType
import com.example.capture.core.result.Result
import com.example.capture.data.datasource.CameraDataSourceImpl
import com.example.capture.data.datasource.FileSystemDataSourceImpl
import com.example.capture.data.datasource.GestureDataSourceImpl
import com.example.capture.data.repository.CameraRepository
import com.example.capture.data.repository.GestureRepository
import com.example.capture.domain.model.CameraActionResult
import com.example.capture.domain.model.CameraState
import com.example.capture.domain.usecase.ProcessGestureUseCase
import com.example.capture.presentation.ui.GestureCameraUiState
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.capture.domain.model.GestureResult as DomainGestureResult
import com.example.capture.domain.model.GestureType as DomainGestureType

class GestureCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraRepository: CameraRepository = CameraRepository(
        CameraDataSourceImpl(application),
        FileSystemDataSourceImpl(application)
    )
    private val gestureRepository: GestureRepository = GestureRepository(
        GestureDataSourceImpl()
    )
    private val processGestureUseCase: ProcessGestureUseCase = ProcessGestureUseCase(
        cameraRepository,
        application
    )

    private val _uiState = MutableStateFlow(GestureCameraUiState())
    val uiState: StateFlow<GestureCameraUiState> = _uiState.asStateFlow()

    val cameraState: StateFlow<CameraState> = cameraRepository.cameraState

    val gestureState = gestureRepository.gestureState

    val gestureProcessingState = processGestureUseCase.uiState

    private val _errorState = MutableStateFlow<AppError?>(null)

    private val _isLoading = MutableStateFlow(false)

    init {
        initializeGestureDetection()

        observeCameraState()

        observeGestureState()

        observeGestureProcessingState()
    }

    private fun initializeGestureDetection() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = gestureRepository.initializeGestureDetection()
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            gestureDetectionInitialized = true
                        )
                    },
                    onError = { error ->
                        _errorState.value = error
                    }
                )
            } catch (e: Exception) {
                _errorState.value = AppError.GenericError.UnexpectedError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeCameraState() {
        viewModelScope.launch {
            cameraState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    cameraState = state,
                    isRecording = state.isRecording,
                    isLoading = state.isLoading
                )
            }
        }
    }

    private fun observeGestureState() {
        viewModelScope.launch {
            gestureState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    currentGesture = GestureResult(
                        gestureType = GestureType.valueOf(state.currentGesture.gestureType.name),
                        confidence = state.currentGesture.confidence
                    ),
                    isDetecting = state.isDetecting,
                    gestureDetectionEnabled = state.isEnabled
                )
            }
        }
    }

    private fun observeGestureProcessingState() {
        viewModelScope.launch {
            gestureProcessingState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    showCooldown = state.showCooldown,
                    cooldownMessage = state.cooldownMessage
                )
            }
        }
    }

    fun initializeCamera(
        camera: androidx.camera.core.Camera?,
        imageCapture: androidx.camera.core.ImageCapture?,
        videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = cameraRepository.initializeCamera(camera, imageCapture, videoCapture)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            cameraInitialized = true
                        )
                    },
                    onError = { error ->
                        _errorState.value = error
                    }
                )
            } catch (e: Exception) {
                _errorState.value = AppError.GenericError.UnexpectedError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processLandmarks(result: HandLandmarkerResult) {
        viewModelScope.launch {
            try {
                val gestureResult = gestureRepository.processLandmarks(result)
                gestureResult.fold(
                    onSuccess = { gesture ->
                        if (gesture.gestureType != DomainGestureType.NONE) {
                            processGestureForAction(gesture)
                        }
                    },
                    onError = { error ->
                        _errorState.value = error
                    }
                )
            } catch (e: Exception) {
                _errorState.value = AppError.GenericError.UnexpectedError(e)
            }
        }
    }

    suspend fun processGestureForAction(gesture: DomainGestureResult): Result<CameraActionResult> {
        return try {
            val result = processGestureUseCase.processGesture(gesture)
            result.fold(
                onSuccess = { actionResult ->
                    _uiState.value = _uiState.value.copy(
                        lastActionResult = actionResult,
                        showActionFeedback = true
                    )

                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(
                            showActionFeedback = false
                        )
                    }

                    Result.Success(actionResult)
                },
                onError = { error ->
                    if (error is AppError.GenericError.BusinessLogicError) {
                    } else {
                        _errorState.value = error
                    }
                    Result.Error(error)
                }
            )
        } catch (e: Exception) {
            _errorState.value = AppError.GenericError.UnexpectedError(e)
            Result.Error(AppError.GenericError.UnexpectedError(e))
        }
    }


    fun resetGalleryState() {
        cameraRepository.resetGalleryState()
    }


    val gestureResult: StateFlow<GestureResult> =
        gestureState.map { state ->
            GestureResult(
                gestureType = GestureType.valueOf(state.currentGesture.gestureType.name),
                confidence = state.currentGesture.confidence
            )
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = GestureResult(GestureType.NONE, 0f)
        )


    fun setCamera(
        camera: androidx.camera.core.Camera?,
        imageCapture: androidx.camera.core.ImageCapture?,
        videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?
    ) {
        initializeCamera(camera, imageCapture, videoCapture)
    }

    override fun onCleared() {
        super.onCleared()
        cameraRepository.cleanup()
        gestureRepository.cleanup()
    }
}

