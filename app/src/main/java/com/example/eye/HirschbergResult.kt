package com.example.eye

data class SingleEyeHirschbergResult(
    val valid: Boolean = false,
    val irisCenterX: Float = 0f,
    val irisCenterY: Float = 0f,
    val reflectionX: Float = 0f,
    val reflectionY: Float = 0f,
    val dlrX: Float = 0f,
    val dlrY: Float = 0f,
    val reason: String = "계산 전"
)

data class HirschbergResult(
    val valid: Boolean = false,
    val left: SingleEyeHirschbergResult = SingleEyeHirschbergResult(),
    val right: SingleEyeHirschbergResult = SingleEyeHirschbergResult(),
    val horizontalDiff: Float = 0f,
    val verticalDiff: Float = 0f,
    val score: Float = 0f,
    val suspected: Boolean = false,
    val reason: String = "반사광 검사 전"
)