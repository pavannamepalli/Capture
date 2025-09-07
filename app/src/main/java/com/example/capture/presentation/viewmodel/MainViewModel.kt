package com.example.capture.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.core.error.AppError
import com.example.capture.core.ml.HandLandmarkerHelper
import com.example.capture.data.datasource.GestureDataSourceImpl
import com.example.capture.data.repository.GestureRepository
import com.example.capture.presentation.ui.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val gestureRepository: GestureRepository = GestureRepository(
        GestureDataSourceImpl()
    )

    private val _delegate = MutableStateFlow(HandLandmarkerHelper.DELEGATE_CPU)

    private val _minHandDetectionConfidence = MutableStateFlow(
        HandLandmarkerHelper.DEFAULT_HAND_DETECTION_CONFIDENCE
    )

    private val _minHandTrackingConfidence = MutableStateFlow(
        HandLandmarkerHelper.DEFAULT_HAND_TRACKING_CONFIDENCE
    )

    private val _minHandPresenceConfidence = MutableStateFlow(
        HandLandmarkerHelper.DEFAULT_HAND_PRESENCE_CONFIDENCE
    )

    private val _maxHands = MutableStateFlow(HandLandmarkerHelper.DEFAULT_NUM_HANDS)

    private val _uiState = MutableStateFlow(MainUiState())

    private val _errorState = MutableStateFlow<AppError?>(null)

    init {
        initializeGestureDetection()
    }

    private fun initializeGestureDetection() {
        viewModelScope.launch {
            try {
                val result = gestureRepository.initializeGestureDetection()
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isInitialized = true
                        )
                    },
                    onError = { error ->
                        _errorState.value = error
                        _uiState.value = _uiState.value.copy(
                            isInitialized = false
                        )
                    }
                )
            } catch (e: Exception) {
                _errorState.value = AppError.GenericError.UnexpectedError(e)
            }
        }
    }

    fun setDelegate(delegate: Int) {
        _delegate.value = delegate
        initializeGestureDetection()
    }

    fun setMinHandDetectionConfidence(confidence: Float) {
        _minHandDetectionConfidence.value = confidence
    }

    fun setMinHandTrackingConfidence(confidence: Float) {
        _minHandTrackingConfidence.value = confidence
    }

    fun setMinHandPresenceConfidence(confidence: Float) {
        _minHandPresenceConfidence.value = confidence
    }

    fun setMaxHands(maxResults: Int) {
        _maxHands.value = maxResults
    }


    val currentMinHandDetectionConfidence: Float get() = _minHandDetectionConfidence.value
    val currentMinHandTrackingConfidence: Float get() = _minHandTrackingConfidence.value
    val currentMinHandPresenceConfidence: Float get() = _minHandPresenceConfidence.value
    val currentMaxHands: Int get() = _maxHands.value
    val currentDelegate: Int get() = _delegate.value

    override fun onCleared() {
        super.onCleared()
        gestureRepository.cleanup()
    }
}



