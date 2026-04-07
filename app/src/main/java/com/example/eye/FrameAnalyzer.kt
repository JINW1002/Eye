package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FrameAnalyzer(
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val protocolManager: ProtocolManager,
    private val cameraMode: CameraMode,
    private val onResult: (AnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private var fps = 0.0
    private var lastFpsTime = System.currentTimeMillis()

    private val scoreAccumulator = StrabismusScoreAccumulator(maxFrames = 20)

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
                mirrorFrontCamera = cameraMode == CameraMode.FRONT
            )

            val result: FaceLandmarkerResult? = faceLandmarkerHelper.detect(rotatedBitmap)
            val faceDetected = result != null && result.faceLandmarks().isNotEmpty()

            var landmarks: List<PointF> = emptyList()
            var faceBox: RectF? = null

            var leftEyePoints: List<PointF> = emptyList()
            var rightEyePoints: List<PointF> = emptyList()
            var leftIrisPoints: List<PointF> = emptyList()
            var rightIrisPoints: List<PointF> = emptyList()

            var leftEyeRoiRect: RectF? = null
            var rightEyeRoiRect: RectF? = null
            var leftEyeRoiValid = false
            var rightEyeRoiValid = false
            var bothEyeRoiValid = false

            var leftEyeRoiScore = 0f
            var rightEyeRoiScore = 0f
            var roiQualityReason = "ROI 없음"

            var faceCentered = false
            var leftEyeOpenRatio = 0f
            var rightEyeOpenRatio = 0f
            var leftIrisVisible = false
            var rightIrisVisible = false

            var leftEyeCenter: PointF? = null
            var rightEyeCenter: PointF? = null
            var leftIrisCenter: PointF? = null
            var rightIrisCenter: PointF? = null
            var bothEyesReady = false

            var leftIrisNormalizedX = 0f
            var leftIrisNormalizedY = 0f
            var rightIrisNormalizedX = 0f
            var rightIrisNormalizedY = 0f
            var irisHorizontalDiff = 0f
            var irisVerticalDiff = 0f
            var alignmentScore = 0f
            var alignmentSuspected = false
            var alignmentReason = "정렬 계산 전"

            var strabismusScore = 0f
            var strabismusSuspected = false
            var strabismusLabel = "정상"
            var strabismusReason = "판정 전"

            var accumulatedScore = 0f
            var accumulatedFrameCount = 0
            var accumulatedSuspected = false
            var accumulatedLabel = "판정불가"
            var accumulatedReason = "누적 전"

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

                leftEyePoints = FaceMeshIndices.LEFT_EYE.mapNotNull { landmarks.getOrNull(it) }
                rightEyePoints = FaceMeshIndices.RIGHT_EYE.mapNotNull { landmarks.getOrNull(it) }

                leftIrisPoints = if (landmarks.size > FaceMeshIndices.LEFT_IRIS.max()) {
                    FaceMeshIndices.LEFT_IRIS.mapNotNull { landmarks.getOrNull(it) }
                } else {
                    emptyList()
                }

                rightIrisPoints = if (landmarks.size > FaceMeshIndices.RIGHT_IRIS.max()) {
                    FaceMeshIndices.RIGHT_IRIS.mapNotNull { landmarks.getOrNull(it) }
                } else {
                    emptyList()
                }

                val eyeRoiResult = EyeRoiExtractor.extractEyeRois(
                    bitmap = rotatedBitmap,
                    landmarks = landmarks
                )

                leftEyeRoiRect = eyeRoiResult.leftEyeRect
                rightEyeRoiRect = eyeRoiResult.rightEyeRect

                val qualityResult = EyeRoiQualityEvaluator.evaluate(
                    imageWidth = rotatedBitmap.width,
                    imageHeight = rotatedBitmap.height,
                    leftRect = eyeRoiResult.leftEyeRect,
                    rightRect = eyeRoiResult.rightEyeRect,
                    leftBitmap = eyeRoiResult.leftEyeBitmap,
                    rightBitmap = eyeRoiResult.rightEyeBitmap
                )

                leftEyeRoiScore = qualityResult.leftScore
                rightEyeRoiScore = qualityResult.rightScore
                leftEyeRoiValid = qualityResult.leftValid
                rightEyeRoiValid = qualityResult.rightValid
                bothEyeRoiValid = qualityResult.bothValid
                roiQualityReason = qualityResult.reason

                val eyeFeatureResult = EyeFeatureExtractor.extract(
                    leftEyePoints = leftEyePoints,
                    rightEyePoints = rightEyePoints,
                    leftIrisPoints = leftIrisPoints,
                    rightIrisPoints = rightIrisPoints,
                    leftEyeRoiScore = leftEyeRoiScore,
                    rightEyeRoiScore = rightEyeRoiScore
                )

                leftEyeCenter = eyeFeatureResult.left.eyeCenter
                rightEyeCenter = eyeFeatureResult.right.eyeCenter
                leftIrisCenter = eyeFeatureResult.left.irisCenter
                rightIrisCenter = eyeFeatureResult.right.irisCenter
                bothEyesReady = eyeFeatureResult.bothEyesReady

                leftEyeOpenRatio = eyeFeatureResult.left.eyeOpenRatio
                rightEyeOpenRatio = eyeFeatureResult.right.eyeOpenRatio
                leftIrisVisible = eyeFeatureResult.left.irisVisible
                rightIrisVisible = eyeFeatureResult.right.irisVisible

                val alignmentResult = EyeAlignmentAnalyzer.analyze(
                    leftEyePoints = leftEyePoints,
                    rightEyePoints = rightEyePoints,
                    leftIrisCenter = leftIrisCenter,
                    rightIrisCenter = rightIrisCenter
                )

                leftIrisNormalizedX = alignmentResult.left.normalizedX
                leftIrisNormalizedY = alignmentResult.left.normalizedY
                rightIrisNormalizedX = alignmentResult.right.normalizedX
                rightIrisNormalizedY = alignmentResult.right.normalizedY
                irisHorizontalDiff = alignmentResult.horizontalDiff
                irisVerticalDiff = alignmentResult.verticalDiff
                alignmentScore = alignmentResult.alignmentScore
                alignmentSuspected = alignmentResult.suspected
                alignmentReason = alignmentResult.reason

                val strabismusResult = StrabismusScorer.score(
                    bothEyesReady = bothEyesReady,
                    alignmentScore = alignmentScore,
                    irisHorizontalDiff = irisHorizontalDiff,
                    irisVerticalDiff = irisVerticalDiff
                )

                strabismusScore = strabismusResult.score
                strabismusSuspected = strabismusResult.suspected
                strabismusLabel = strabismusResult.label
                strabismusReason = strabismusResult.reason

                val accumulatedResult = scoreAccumulator.addScore(
                    validFrame = bothEyesReady && bothEyeRoiValid,
                    score = strabismusScore
                )

                accumulatedScore = accumulatedResult.averageScore
                accumulatedFrameCount = accumulatedResult.frameCount
                accumulatedSuspected = accumulatedResult.suspected
                accumulatedLabel = accumulatedResult.label
                accumulatedReason = accumulatedResult.reason
            } else {
                val accumulatedResult = scoreAccumulator.currentResult()
                accumulatedScore = accumulatedResult.averageScore
                accumulatedFrameCount = accumulatedResult.frameCount
                accumulatedSuspected = accumulatedResult.suspected
                accumulatedLabel = accumulatedResult.label
                accumulatedReason = accumulatedResult.reason
            }

            val (fullText, requestedCameraMode, requestTorchOn) = protocolManager.update(
                activeCameraMode = cameraMode,
                faceDetected = faceDetected,
                faceCentered = faceCentered,
                bothEyeRoiValid = bothEyeRoiValid,
                leftEyeRoiScore = leftEyeRoiScore,
                rightEyeRoiScore = rightEyeRoiScore,
                roiQualityReason = roiQualityReason,
                leftEyeOpenRatio = leftEyeOpenRatio,
                rightEyeOpenRatio = rightEyeOpenRatio,
                leftIrisVisible = leftIrisVisible,
                rightIrisVisible = rightIrisVisible
            )

            val lines = fullText.split("\n")
            val guideMessage = lines.firstOrNull() ?: ""
            val originalDebugText = lines.drop(1).joinToString("\n")

            val roiDebug = buildString {
                append(originalDebugText)

                if (originalDebugText.isNotBlank()) append("\n")

                append("leftEyeRoiScore: %.2f\n".format(leftEyeRoiScore))
                append("rightEyeRoiScore: %.2f\n".format(rightEyeRoiScore))
                append("bothEyeRoiValid: $bothEyeRoiValid\n")
                append("bothEyesReady: $bothEyesReady\n")
                append("alignmentScore: %.2f\n".format(alignmentScore))
                append("alignmentSuspected: $alignmentSuspected\n")
                append("irisHorizontalDiff: %.3f\n".format(irisHorizontalDiff))
                append("irisVerticalDiff: %.3f\n".format(irisVerticalDiff))
                append("strabismusScore: %.3f\n".format(strabismusScore))
                append("strabismusSuspected: $strabismusSuspected\n")
                append("strabismusLabel: $strabismusLabel\n")
                append("accumulatedScore: %.3f\n".format(accumulatedScore))
                append("accumulatedFrameCount: $accumulatedFrameCount\n")
                append("accumulatedSuspected: $accumulatedSuspected\n")
                append("accumulatedLabel: $accumulatedLabel\n")
                append("alignmentReason: $alignmentReason\n")
                append("strabismusReason: $strabismusReason\n")
                append("accumulatedReason: $accumulatedReason")

                leftIrisCenter?.let {
                    append("\nL-iris-center: (%.1f, %.1f)".format(it.x, it.y))
                }
                rightIrisCenter?.let {
                    append("\nR-iris-center: (%.1f, %.1f)".format(it.x, it.y))
                }

                append("\nL-norm: (%.3f, %.3f)".format(leftIrisNormalizedX, leftIrisNormalizedY))
                append("\nR-norm: (%.3f, %.3f)".format(rightIrisNormalizedX, rightIrisNormalizedY))
            }

            val finalResult = AnalysisResult(
                guideMessage = guideMessage,
                fpsText = "FPS: %.1f".format(fps),
                debugText = roiDebug,
                faceDetected = faceDetected,
                landmarks = landmarks,
                faceBox = faceBox,
                leftEyePoints = leftEyePoints,
                rightEyePoints = rightEyePoints,
                leftIrisPoints = leftIrisPoints,
                rightIrisPoints = rightIrisPoints,
                leftEyeRoiRect = leftEyeRoiRect,
                rightEyeRoiRect = rightEyeRoiRect,
                leftEyeRoiValid = leftEyeRoiValid,
                rightEyeRoiValid = rightEyeRoiValid,
                leftEyeRoiScore = leftEyeRoiScore,
                rightEyeRoiScore = rightEyeRoiScore,
                bothEyeRoiValid = bothEyeRoiValid,
                roiQualityReason = roiQualityReason,
                leftEyeCenter = leftEyeCenter,
                rightEyeCenter = rightEyeCenter,
                leftIrisCenter = leftIrisCenter,
                rightIrisCenter = rightIrisCenter,
                bothEyesReady = bothEyesReady,
                leftIrisNormalizedX = leftIrisNormalizedX,
                leftIrisNormalizedY = leftIrisNormalizedY,
                rightIrisNormalizedX = rightIrisNormalizedX,
                rightIrisNormalizedY = rightIrisNormalizedY,
                irisHorizontalDiff = irisHorizontalDiff,
                irisVerticalDiff = irisVerticalDiff,
                alignmentScore = alignmentScore,
                alignmentSuspected = alignmentSuspected,
                alignmentReason = alignmentReason,
                strabismusScore = strabismusScore,
                strabismusSuspected = strabismusSuspected,
                strabismusLabel = strabismusLabel,
                strabismusReason = strabismusReason,
                accumulatedScore = accumulatedScore,
                accumulatedFrameCount = accumulatedFrameCount,
                accumulatedSuspected = accumulatedSuspected,
                accumulatedLabel = accumulatedLabel,
                accumulatedReason = accumulatedReason,
                imageWidth = rotatedBitmap.width,
                imageHeight = rotatedBitmap.height,
                requestedCameraMode = requestedCameraMode,
                requestTorchOn = requestTorchOn
            )

            onResult(finalResult)
        } catch (e: Exception) {
            onResult(
                AnalysisResult(
                    guideMessage = "분석 중 오류가 발생했습니다.",
                    fpsText = "FPS: %.1f".format(fps),
                    debugText = e.message ?: "unknown error"
                )
            )
        } finally {
            image.close()
        }
    }
}