package com.example.eye

data class EllSegVisibilityResult(
    val available: Boolean = false,
    val irisVisible: Boolean = false,
    val irisAreaRatio: Float = 0f,
    val confidence: Float = 0f,
    val reason: String = "EllSeg 실행 전"
)