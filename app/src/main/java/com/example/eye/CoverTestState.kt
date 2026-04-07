package com.example.eye

data class CoverTestState(
    val baselineHorizontalDiff: Float? = null,
    val baselineVerticalDiff: Float? = null,
    val rightCoverShift: Float = 0f,
    val leftCoverShift: Float = 0f,
    val rightCoverRecorded: Boolean = false,
    val leftCoverRecorded: Boolean = false,
    val coverScore: Float = 0f,
    val suspected: Boolean = false,
    val reason: String = "가림 검사 계산 전"
)