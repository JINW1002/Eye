package com.example.eye

data class HandCoverResult(
    val rightEyeCovered: Boolean = false,
    val leftEyeCovered: Boolean = false,
    val detectedHandCount: Int = 0,
    val reason: String = "손 검출 전"
)