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
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)