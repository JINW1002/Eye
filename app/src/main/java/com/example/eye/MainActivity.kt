package com.example.eye

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.LinearLayout
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
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: FaceOverlayView
    private lateinit var guideText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var fpsText: TextView
    private lateinit var debugText: TextView

    private lateinit var resultCard: LinearLayout
    private lateinit var resultTitleText: TextView
    private lateinit var resultValueText: TextView
    private lateinit var resultReasonText: TextView

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private lateinit var csvLogger: ScreeningCsvLogger

    private val protocolManager = ProtocolManager()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastSpokenGuide = ""
    private var lastSpeakTime = 0L
    private val speakInterval = 3000L

    private val requestCodeCamera = 100

    private var currentCameraMode = CameraMode.FRONT
    private var currentTorchOn = false

    private var boundCamera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var hasSavedFinalResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        guideText = findViewById(R.id.guideText)
        subtitleText = findViewById(R.id.subtitleText)
        fpsText = findViewById(R.id.fpsText)
        debugText = findViewById(R.id.debugText)

        resultCard = findViewById(R.id.resultCard)
        resultTitleText = findViewById(R.id.resultTitleText)
        resultValueText = findViewById(R.id.resultValueText)
        resultReasonText = findViewById(R.id.resultReasonText)

        cameraExecutor = Executors.newSingleThreadExecutor()

        faceLandmarkerHelper = FaceLandmarkerHelper(this)
        faceLandmarkerHelper.setup()

        csvLogger = ScreeningCsvLogger(this)
        tts = TextToSpeech(this, this)

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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speakGuideMessage(message: String) {
        if (!ttsReady) return
        if (message.isBlank()) return

        val now = System.currentTimeMillis()

        if (message != lastSpokenGuide || now - lastSpeakTime >= speakInterval) {
            lastSpokenGuide = message
            lastSpeakTime = now
            tts?.stop()
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "guide_message"
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
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor,
                    FrameAnalyzer(faceLandmarkerHelper, protocolManager, mode) { result ->
                        runOnUiThread {
                            guideText.text = result.guideMessage
                            fpsText.text = result.fpsText
                            subtitleText.text = buildSubtitle(result)

                            // 기본은 숨김
                            debugText.visibility = View.GONE

                            overlayView.setResults(
                                landmarks = result.landmarks,
                                faceBox = result.faceBox,
                                leftEyePoints = result.leftEyePoints,
                                rightEyePoints = result.rightEyePoints,
                                leftIrisPoints = result.leftIrisPoints,
                                rightIrisPoints = result.rightIrisPoints,
                                leftEyeRoiRect = result.leftEyeRoiRect,
                                rightEyeRoiRect = result.rightEyeRoiRect,
                                imageWidth = result.imageWidth,
                                imageHeight = result.imageHeight,
                                faceDetected = result.faceDetected
                            )

                            if (result.isFinalResult) {
                                resultCard.visibility = View.VISIBLE
                                resultValueText.text = result.finalResultLabel
                                resultReasonText.text = result.finalResultReason
                            } else {
                                resultCard.visibility = View.GONE
                            }

                            speakGuideMessage(result.guideMessage)

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

                            if (result.isFinalResult && !hasSavedFinalResult) {
                                csvLogger.append(result)
                                hasSavedFinalResult = true
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

    private fun buildSubtitle(result: AnalysisResult): String {
        return if (result.isFinalResult) {
            "최종 점수: %.2f".format(result.finalResultScore)
        } else {
            "누적 점수: %.2f | 유효 프레임: %d".format(
                result.accumulatedScore,
                result.accumulatedFrameCount
            )
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
        permissions: Array<String>,
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
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}