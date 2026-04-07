package com.example.eye

data class StrabismusScoreResult(
    val score: Float = 0f,
    val suspected: Boolean = false,
    val label: String = "정상",
    val reason: String = "계산 전"
)