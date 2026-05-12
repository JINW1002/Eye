package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sqrt

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

        val horizontalDiff = abs(leftAlignment.gazeX - rightAlignment.gazeX)
        val verticalDiff = abs(leftAlignment.gazeY - rightAlignment.gazeY)

        val gazeVectorDiff = sqrt(
            horizontalDiff * horizontalDiff +
                    verticalDiff * verticalDiff
        )

        val score = (
                (horizontalDiff / 0.22f).coerceIn(0f, 1f) * 0.65f +
                        (verticalDiff / 0.18f).coerceIn(0f, 1f) * 0.25f +
                        (gazeVectorDiff / 0.25f).coerceIn(0f, 1f) * 0.10f
                ).coerceIn(0f, 1f)

        val suspected = horizontalDiff >= 0.13f ||
                verticalDiff >= 0.11f ||
                gazeVectorDiff >= 0.17f

        val reason = when {
            suspected && horizontalDiff >= verticalDiff ->
                "좌우 눈의 시선 벡터 수평 차이가 큽니다."
            suspected ->
                "좌우 눈의 시선 벡터 수직 차이가 큽니다."
            else ->
                "좌우 눈 시선 방향이 비교적 안정적입니다."
        }

        return EyeAlignmentFeatures(
            left = leftAlignment,
            right = rightAlignment,
            horizontalDiff = horizontalDiff,
            verticalDiff = verticalDiff,
            gazeVectorDiff = gazeVectorDiff,
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

        val eyeCenterX = eyeRect.centerX()
        val eyeCenterY = eyeRect.centerY()

        val normalizedX = ((irisCenter.x - eyeRect.left) / width).coerceIn(0f, 1f)
        val normalizedY = ((irisCenter.y - eyeRect.top) / height).coerceIn(0f, 1f)

        val gazeX = ((irisCenter.x - eyeCenterX) / width).coerceIn(-1f, 1f)
        val gazeY = ((irisCenter.y - eyeCenterY) / height).coerceIn(-1f, 1f)

        return SingleEyeAlignment(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            gazeX = gazeX,
            gazeY = gazeY,
            valid = true
        )
    }
}