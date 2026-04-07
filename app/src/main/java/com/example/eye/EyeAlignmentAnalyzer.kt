package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs

object EyeAlignmentAnalyzer {

    fun analyze(
        leftEyePoints: List<PointF>,
        rightEyePoints: List<PointF>,
        leftIrisCenter: PointF?,
        rightIrisCenter: PointF?
    ): EyeAlignmentFeatures {

        val leftRect = FaceMathUtils.boundingBox(leftEyePoints)
        val rightRect = FaceMathUtils.boundingBox(rightEyePoints)

        val leftAlignment = computeSingle(leftRect, leftIrisCenter)
        val rightAlignment = computeSingle(rightRect, rightIrisCenter)

        if (!leftAlignment.valid || !rightAlignment.valid) {
            return EyeAlignmentFeatures(
                left = leftAlignment,
                right = rightAlignment,
                reason = "좌우 눈 또는 홍채 중심 계산이 충분하지 않습니다."
            )
        }

        val horizontalDiff = abs(leftAlignment.normalizedX - rightAlignment.normalizedX)
        val verticalDiff = abs(leftAlignment.normalizedY - rightAlignment.normalizedY)

        val score = (
                (horizontalDiff / 0.20f).coerceIn(0f, 1f) * 0.7f +
                        (verticalDiff / 0.15f).coerceIn(0f, 1f) * 0.3f
                ).coerceIn(0f, 1f)

        val suspected = horizontalDiff >= 0.12f || verticalDiff >= 0.10f

        val reason = when {
            suspected && horizontalDiff >= verticalDiff ->
                "좌우 눈의 수평 정렬 차이가 큽니다."
            suspected ->
                "좌우 눈의 수직 정렬 차이가 큽니다."
            else ->
                "좌우 눈 정렬이 비교적 안정적입니다."
        }

        return EyeAlignmentFeatures(
            left = leftAlignment,
            right = rightAlignment,
            horizontalDiff = horizontalDiff,
            verticalDiff = verticalDiff,
            alignmentScore = score,
            suspected = suspected,
            reason = reason
        )
    }

    private fun computeSingle(
        eyeRect: RectF?,
        irisCenter: PointF?
    ): SingleEyeAlignment {
        if (eyeRect == null || irisCenter == null) {
            return SingleEyeAlignment(valid = false)
        }

        val width = eyeRect.width()
        val height = eyeRect.height()

        if (width <= 1f || height <= 1f) {
            return SingleEyeAlignment(valid = false)
        }

        val normalizedX = ((irisCenter.x - eyeRect.left) / width).coerceIn(0f, 1f)
        val normalizedY = ((irisCenter.y - eyeRect.top) / height).coerceIn(0f, 1f)

        return SingleEyeAlignment(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            valid = true
        )
    }
}