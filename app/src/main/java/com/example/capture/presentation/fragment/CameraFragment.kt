package com.example.capture.presentation.fragment

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.capture.R
import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.HandLandmarkerHelper
import com.example.capture.databinding.FragmentCameraBinding
import com.example.capture.presentation.ui.GestureCameraUiState
import com.example.capture.presentation.viewmodel.GestureCameraViewModel
import com.example.capture.presentation.viewmodel.MainViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), HandLandmarkerHelper.LandmarkerListener {


    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val gestureViewModel: GestureCameraViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    private lateinit var backgroundExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[CAMERA] ?: false
            val audioGranted = permissions[RECORD_AUDIO] ?: false

            if (cameraGranted && audioGranted) {
                fragmentCameraBinding.viewFinder.post {
                    setUpCamera()
                }
            } else {
                val missingPermissions = mutableListOf<String>()
                if (!cameraGranted) missingPermissions.add(getString(R.string.permission_camera))
                if (!audioGranted) missingPermissions.add(getString(R.string.permission_microphone))
            }
        }

    override fun onResume() {
        super.onResume()
        gestureViewModel.resetGalleryState()

        if (!hasAllPermissions()) {
            requestCameraPermission()
        }

        backgroundExecutor.execute {
            if (handLandmarkerHelper.isClose()) {
                handLandmarkerHelper.setupHandLandmarker()
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        when {
            hasAllPermissions() -> {
                fragmentCameraBinding.viewFinder.post {
                    setUpCamera()
                }
            }

            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        CAMERA,
                        RECORD_AUDIO
                    )
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::handLandmarkerHelper.isInitialized) {
            viewModel.setMaxHands(handLandmarkerHelper.maxNumHands)
            viewModel.setMinHandDetectionConfidence(handLandmarkerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(handLandmarkerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(handLandmarkerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(handLandmarkerHelper.currentDelegate)

            backgroundExecutor.execute { handLandmarkerHelper.clearHandLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        if (PermissionsFragment.hasPermissions(requireContext())) {
            fragmentCameraBinding.viewFinder.post {
                setUpCamera()
            }
        } else {
            requestCameraPermission()
        }

        backgroundExecutor.execute {
            handLandmarkerHelper = HandLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                maxNumHands = viewModel.currentMaxHands,
                currentDelegate = viewModel.currentDelegate,
                handLandmarkerHelperListener = this
            )
        }

        setupGestureObservers()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        fragmentCameraBinding.fabGestureInfo.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_gesture_info)
        }
    }


    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraProvider = cameraProvider
            ?: throw IllegalStateException(getString(R.string.error_camera_initialization_failed))

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectHand(image)
                    }
                }

        videoCapture = VideoCapture.Builder(Recorder.Builder().build())
            .build()

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, videoCapture, imageAnalyzer
            )

            gestureViewModel.setCamera(camera, imageCapture, videoCapture)

            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
        }
    }

    private fun detectHand(imageProxy: ImageProxy) {
        handLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    private fun setupGestureObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            gestureViewModel.gestureResult.collect { gestureResult ->
                updateGestureDisplay(gestureResult)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            gestureViewModel.uiState.collect { uiState ->
                updateActionFeedback(uiState)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            gestureViewModel.cameraState.collect { cameraState ->
                if (cameraState.cameraFacing != cameraFacing) {
                    cameraFacing = cameraState.cameraFacing
                    bindCameraUseCases()
                }


                fragmentCameraBinding.gestureOverlay.updateRecordingState(cameraState.isRecording)
            }
        }

    }

    private fun updateGestureDisplay(gestureResult: GestureResult) {
        fragmentCameraBinding.gestureOverlay.updateGesture(gestureResult)

    }

    private fun updateActionFeedback(uiState: GestureCameraUiState) {
        uiState.lastActionResult?.let { result ->
            if (uiState.showActionFeedback) {
                val message = if (result.success) {
                    result.message ?: getString(R.string.ui_action_completed)
                } else {
                    getString(R.string.ui_error_prefix, result.message ?: "")
                }

                fragmentCameraBinding.gestureOverlay.updateAction(message)


            }
        }

        updateCooldownStatus(uiState)
    }

    private fun updateCooldownStatus(uiState: GestureCameraUiState) {
        if (uiState.showCooldown && uiState.cooldownMessage != null) {
            fragmentCameraBinding.gestureOverlay.updateCooldown(uiState.cooldownMessage!!)

            if (uiState.cooldownMessage!!.contains("blocked while recording")) {
            }
        } else {
            fragmentCameraBinding.gestureOverlay.updateCooldown(null)
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(
        resultBundle: HandLandmarkerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                fragmentCameraBinding.overlay.invalidate()

                if (resultBundle.results.isNotEmpty()) {
                    gestureViewModel.processLandmarks(resultBundle.results.first())
                }
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
        }
    }
}
