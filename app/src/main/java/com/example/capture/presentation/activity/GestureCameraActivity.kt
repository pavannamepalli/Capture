package com.example.capture.presentation.activity

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import com.example.capture.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.capture.architecture.FrameProcessor
import com.example.capture.architecture.GestureHandler
import com.example.capture.architecture.UIUpdater
import com.example.capture.presentation.ui.UIUpdaterState
import com.example.capture.camera.CameraController
import com.example.capture.core.error.AppError
import com.example.capture.core.error.getUserMessage
import com.example.capture.core.ml.GestureResult
import com.example.capture.databinding.ActivityGestureCameraBinding
import com.example.capture.performance.PerformanceMonitor
import com.example.capture.presentation.viewmodel.GestureCameraViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureCameraActivity : AppCompatActivity() {


    private lateinit var binding: ActivityGestureCameraBinding

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var frameProcessor: FrameProcessor
    private lateinit var gestureHandler: GestureHandler
    private lateinit var uiUpdater: UIUpdater
    private lateinit var cameraController: CameraController

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var backgroundExecutor: ExecutorService

    private val viewModel: GestureCameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGestureCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeArchitecture()

        if (hasCameraPermission()) {
            setupCamera()
        } else {
            requestCameraPermission()
        }

        setupUIObservers()

    }

    private fun initializeArchitecture() {
        backgroundExecutor = Executors.newSingleThreadExecutor()

        performanceMonitor = PerformanceMonitor()

        frameProcessor = FrameProcessor(performanceMonitor)
        frameProcessor.setOnFrameProcessedCallback { bitmap ->
            gestureHandler.processFrame(bitmap)
        }

        gestureHandler = GestureHandler(this, performanceMonitor)
        gestureHandler.setOnGestureDetectedCallback { gestureResult ->
            uiUpdater.updateGestureResult(gestureResult)

            processGestureWithNewArchitecture(gestureResult)
        }

        uiUpdater = UIUpdater()

        cameraController = CameraController()

        gestureHandler.initialize()

    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(cameraFacing)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(backgroundExecutor) { imageProxy ->
                    frameProcessor.processFrame(
                        imageProxy,
                        cameraFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }
            }

        videoCapture = VideoCapture.Builder(Recorder.Builder().build())
            .build()

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture,
                imageAnalyzer
            )

            cameraController.setCamera(camera, imageCapture, videoCapture)

            viewModel.initializeCamera(camera, imageCapture, videoCapture)

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)


        } catch (e: Exception) {
        }
    }

    private fun setupUIObservers() {
        lifecycleScope.launch {
            uiUpdater.uiState.collect { uiState ->
                updateUI(uiState)
            }
        }


        lifecycleScope.launch {
            uiUpdater.gestureFeedback.collect { feedback ->
                if (feedback.isVisible) {
                    binding.gestureOverlay.updateGesture(
                        GestureResult(feedback.gestureType, feedback.confidence, feedback.timestamp)
                    )
                }
            }
        }
    }

    private fun updateUI(uiState: UIUpdaterState) {
        binding.gestureOverlay.updateRecordingState(uiState.isRecording)

        if (uiState.showActionFeedback && uiState.actionMessage != null) {
            binding.gestureOverlay.updateAction(uiState.actionMessage!!)
        }

        if (uiState.showCooldown && uiState.cooldownMessage != null) {
            binding.gestureOverlay.updateCooldown(uiState.cooldownMessage)
        } else {
            binding.gestureOverlay.updateCooldown(null)
        }
    }



    private fun processGestureWithNewArchitecture(gestureResult: GestureResult) {
        lifecycleScope.launch {
            try {

                val domainGesture = com.example.capture.domain.model.GestureResult(
                    gestureType = com.example.capture.domain.model.GestureType.valueOf(gestureResult.gestureType.name),
                    confidence = gestureResult.confidence
                )


                val result = viewModel.processGestureForAction(domainGesture)

                result.fold(
                    onSuccess = { actionResult ->
                        if (actionResult.success) {
                            val message = actionResult.message ?: getString(R.string.ui_action_completed)
                            android.widget.Toast.makeText(this@GestureCameraActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        
                        uiUpdater.updateActionFeedback(
                            action = actionResult.action.name,
                            success = actionResult.success,
                            message = actionResult.message
                                ?: getString(R.string.ui_action_completed)
                        )

                        uiUpdater.updateCameraState(
                            isRecording = viewModel.cameraState.value.isRecording,
                            cameraFacing = viewModel.cameraState.value.cameraFacing,
                            zoomRatio = viewModel.cameraState.value.zoomRatio
                        )

                    },
                    onError = { error ->
                        val errorMessage = when (error) {
                            is AppError.GenericError.BusinessLogicError -> error.message
                            else -> getString(
                                R.string.ui_error_prefix,
                                error.getUserMessage(this@GestureCameraActivity)
                            )
                        }

                        uiUpdater.updateActionFeedback(
                            action = getString(R.string.action_name_gesture),
                            success = false,
                            message = errorMessage
                        )

                    }
                )

            } catch (e: Exception) {
                val errorMessage = getString(R.string.ui_error_prefix, e.message ?: "")
                
                uiUpdater.updateActionFeedback(
                    action = getString(R.string.action_name_unknown),
                    success = false,
                    message = errorMessage
                )
            }
        }
    }



    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
    }

    override fun onDestroy() {
        super.onDestroy()

        frameProcessor.cleanup()
        gestureHandler.cleanup()
        cameraController.cleanup()
        backgroundExecutor.shutdown()

    }
}
