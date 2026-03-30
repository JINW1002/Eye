package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FrameAnalyzer(
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val onResult: (AnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val protocolManager = ProtocolManager()

    private var frameCount = 0
    private var fps = 0.0
    private var lastFpsTime = System.currentTimeMillis()

    override fun analyze(image: ImageProxy) {
        frameCount++

        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsTime
        if (elapsed >= 1000) {
            fps = frameCount * 1000.0 / elapsed
            frameCount = 0
            lastFpsTime = now
        }

        try {
            val bitmap = FaceLandmarkerHelper.imageProxyToBitmap(image)
            val rotatedBitmap = FaceLandmarkerHelper.rotateBitmap(
                bitmap = bitmap,
                rotationDegrees = image.imageInfo.rotationDegrees,
                mirrorFrontCamera = true
            )

            val result: FaceLandmarkerResult? = faceLandmarkerHelper.detect(rotatedBitmap)
            val faceDetected = result != null && result.faceLandmarks().isNotEmpty()

            var landmarks: List<PointF> = emptyList()
            var faceBox: RectF? = null
            var leftEyePoints: List<PointF> = emptyList()
            var rightEyePoints: List<PointF> = emptyList()
            var leftIrisPoints: List<PointF> = emptyList()
            var rightIrisPoints: List<PointF> = emptyList()
            var faceCentered = false
            var leftEyeOpenRatio = 0f
            var rightEyeOpenRatio = 0f
            var leftIrisVisible = false
            var rightIrisVisible = false

            if (faceDetected) {
                val faceLandmarks = result!!.faceLandmarks()[0]

                landmarks = faceLandmarks.map {
                    FaceLandmarkerHelper.normalizedToPixelPoint(
                        it.x(),
                        it.y(),
                        rotatedBitmap.width,
                        rotatedBitmap.height
                    )
                }

                faceBox = FaceMathUtils.boundingBox(landmarks)
                if (faceBox != null) {
                    faceCentered = FaceMathUtils.isFaceCentered(
                        faceBox,
                        rotatedBitmap.width,
                        rotatedBitmap.height
                    )
                }

                leftEyePoints = FaceMeshIndices.LEFT_EYE.map { landmarks[it] }
                rightEyePoints = FaceMeshIndices.RIGHT_EYE.map { landmarks[it] }

                leftIrisPoints = if (landmarks.size > FaceMeshIndices.LEFT_IRIS.max()) {
                    FaceMeshIndices.LEFT_IRIS.map { landmarks[it] }
                } else {
                    emptyList()
                }

                rightIrisPoints = if (landmarks.size > FaceMeshIndices.RIGHT_IRIS.max()) {
                    FaceMeshIndices.RIGHT_IRIS.map { landmarks[it] }
                } else {
                    emptyList()
                }

                leftEyeOpenRatio = FaceMathUtils.eyeOpenRatio(leftEyePoints)
                rightEyeOpenRatio = FaceMathUtils.eyeOpenRatio(rightEyePoints)

                leftIrisVisible = leftIrisPoints.size >= 5
                rightIrisVisible = rightIrisPoints.size >= 5
            }

            val (fullText, requestedCameraMode, requestTorchOn) = protocolManager.update(
                faceDetected = faceDetected,
                faceCentered = faceCentered,
                leftEyeOpenRatio = leftEyeOpenRatio,
                rightEyeOpenRatio = rightEyeOpenRatio,
                leftIrisVisible = leftIrisVisible,
                rightIrisVisible = rightIrisVisible
            )

            val lines = fullText.split("\n")
            val guideMessage = lines.firstOrNull() ?: ""
            val debugText = lines.drop(1).joinToString("\n")

            val finalResult = AnalysisResult(
                guideMessage = guideMessage,
                fpsText = "FPS: %.1f".format(fps),
                debugText = debugText,
                faceDetected = faceDetected,
                landmarks = landmarks,
                faceBox = faceBox,
                leftEyePoints = leftEyePoints,
                rightEyePoints = rightEyePoints,
                leftIrisPoints = leftIrisPoints,
                rightIrisPoints = rightIrisPoints,
                imageWidth = rotatedBitmap.width,
                imageHeight = rotatedBitmap.height,
                requestedCameraMode = requestedCameraMode,
                requestTorchOn = requestTorchOn
            )

            onResult(finalResult)
        } catch (e: Exception) {
            onResult(
                AnalysisResult(
                    guideMessage = "분석 중 오류가 발생했습니다",
                    fpsText = "FPS: %.1f".format(fps),
                    debugText = e.message ?: "unknown error"
                )
            )
        } finally {
            image.close()
        }
    }
}