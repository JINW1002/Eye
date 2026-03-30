package com.example.eye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: FaceOverlayView
    private lateinit var guideText: TextView
    private lateinit var fpsText: TextView
    private lateinit var debugText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper

    private val requestCodeCamera = 100

    private var currentCameraMode = CameraMode.FRONT
    private var currentTorchOn = false
    private var boundCamera: Camera? = null
    private var cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        guideText = findViewById(R.id.guideText)
        fpsText = findViewById(R.id.fpsText)
        debugText = findViewById(R.id.debugText)

        cameraExecutor = Executors.newSingleThreadExecutor()

        faceLandmarkerHelper = FaceLandmarkerHelper(this)
        faceLandmarkerHelper.setup()

        if (allPermissionsGranted()) {
            startCamera(currentCameraMode)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                requestCodeCamera
            )
        }
    }

    private fun selectorFor(mode: CameraMode): CameraSelector {
        return when (mode) {
            CameraMode.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraMode.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun startCamera(mode: CameraMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindUseCases(mode)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindUseCases(mode: CameraMode) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    FrameAnalyzer(faceLandmarkerHelper) { result ->
                        runOnUiThread {
                            guideText.text = result.guideMessage
                            fpsText.text = result.fpsText
                            debugText.text = result.debugText

                            overlayView.setResults(
                                landmarks = result.landmarks,
                                faceBox = result.faceBox,
                                leftEyePoints = result.leftEyePoints,
                                rightEyePoints = result.rightEyePoints,
                                leftIrisPoints = result.leftIrisPoints,
                                rightIrisPoints = result.rightIrisPoints,
                                imageWidth = result.imageWidth,
                                imageHeight = result.imageHeight,
                                faceDetected = result.faceDetected
                            )

                            if (result.requestedCameraMode != currentCameraMode) {
                                currentCameraMode = result.requestedCameraMode
                                bindUseCases(currentCameraMode)
                                return@runOnUiThread
                            }

                            if (result.requestTorchOn != currentTorchOn) {
                                currentTorchOn = result.requestTorchOn
                                if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                                    boundCamera?.cameraControl?.enableTorch(currentTorchOn)
                                }
                            }
                        }
                    }
                )
            }

        try {
            provider.unbindAll()
            boundCamera = provider.bindToLifecycle(
                this,
                selectorFor(mode),
                preview,
                imageAnalyzer
            )

            if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                boundCamera?.cameraControl?.enableTorch(currentTorchOn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestCodeCamera) {
            if (allPermissionsGranted()) {
                startCamera(currentCameraMode)
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceLandmarkerHelper.clear()
        cameraExecutor.shutdown()
    }
}