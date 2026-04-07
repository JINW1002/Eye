package com.example.eye

import android.graphics.PointF
import android.graphics.RectF

data class AnalysisResult(
    val guideMessage: String,
    val fpsText: String = "",
    val debugText: String = "",
    val faceDetected: Boolean = false,

    val landmarks: List<PointF> = emptyList(),
    val faceBox: RectF? = null,

    val leftEyePoints: List<PointF> = emptyList(),
    val rightEyePoints: List<PointF> = emptyList(),
    val leftIrisPoints: List<PointF> = emptyList(),
    val rightIrisPoints: List<PointF> = emptyList(),

    val leftEyeRoiRect: RectF? = null,
    val rightEyeRoiRect: RectF? = null,
    val leftEyeRoiValid: Boolean = false,
    val rightEyeRoiValid: Boolean = false,

    val leftEyeRoiScore: Float = 0f,
    val rightEyeRoiScore: Float = 0f,
    val bothEyeRoiValid: Boolean = false,
    val roiQualityReason: String = "",

    val leftEyeCenter: PointF? = null,
    val rightEyeCenter: PointF? = null,
    val leftIrisCenter: PointF? = null,
    val rightIrisCenter: PointF? = null,
    val bothEyesReady: Boolean = false,

    val leftIrisNormalizedX: Float = 0f,
    val leftIrisNormalizedY: Float = 0f,
    val rightIrisNormalizedX: Float = 0f,
    val rightIrisNormalizedY: Float = 0f,
    val irisHorizontalDiff: Float = 0f,
    val irisVerticalDiff: Float = 0f,
    val alignmentScore: Float = 0f,
    val alignmentSuspected: Boolean = false,
    val alignmentReason: String = "",

    val reflectionScore: Float = 0f,
    val reflectionSuspected: Boolean = false,
    val reflectionReason: String = "",

    val rightCoverShift: Float = 0f,
    val leftCoverShift: Float = 0f,
    val coverScore: Float = 0f,
    val coverSuspected: Boolean = false,
    val coverReason: String = "",

    val strabismusScore: Float = 0f,
    val strabismusSuspected: Boolean = false,
    val strabismusLabel: String = "정상",
    val strabismusReason: String = "",

    val accumulatedScore: Float = 0f,
    val accumulatedFrameCount: Int = 0,
    val accumulatedSuspected: Boolean = false,
    val accumulatedLabel: String = "판정불가",
    val accumulatedReason: String = "",

    val isFinalResult: Boolean = false,
    val finalResultLabel: String = "판정불가",
    val finalResultScore: Float = 0f,
    val finalResultReason: String = "",

    val imageWidth: Int = 0,
    val imageHeight: Int = 0,

    val requestedCameraMode: CameraMode = CameraMode.FRONT,
    val requestTorchOn: Boolean = false
)