package com.example.eye

data class SingleEyeAlignment(
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
    val valid: Boolean = false
)

data class EyeAlignmentFeatures(
    val left: SingleEyeAlignment = SingleEyeAlignment(),
    val right: SingleEyeAlignment = SingleEyeAlignment(),
    val horizontalDiff: Float = 0f,
    val verticalDiff: Float = 0f,
    val alignmentScore: Float = 0f,
    val suspected: Boolean = false,
    val reason: String = "정렬 계산 불가"
)