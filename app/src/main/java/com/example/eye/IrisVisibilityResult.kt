package com.example.eye

data class IrisVisibilityResult(
    val available: Boolean = false,
    val irisVisible: Boolean = false,
    val irisAreaRatio: Float = 0f,
    val pupilAreaRatio: Float = 0f,
    val reason: String = "Iris segmentation 실행 전"
)