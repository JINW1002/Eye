package com.example.eye

data class AccumulatedScoreResult(
    val averageScore: Float = 0f,
    val frameCount: Int = 0,
    val suspected: Boolean = false,
    val label: String = "판정불가",
    val reason: String = "누적 전"
)