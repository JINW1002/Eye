package com.example.eye

data class ReflectionScoreResult(
    val score: Float = 0f,
    val suspected: Boolean = false,
    val reason: String = "반사광 계산 전"
)