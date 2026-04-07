package com.example.eye

import android.graphics.PointF

data class SingleEyeFeature(
    val eyeCenter: PointF? = null,
    val irisCenter: PointF? = null,
    val irisVisible: Boolean = false,
    val eyeOpenRatio: Float = 0f,
    val qualityScore: Float = 0f
)

data class EyeFeatureResult(
    val left: SingleEyeFeature = SingleEyeFeature(),
    val right: SingleEyeFeature = SingleEyeFeature(),
    val bothEyesReady: Boolean = false
)