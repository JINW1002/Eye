package com.example.eye

import android.graphics.PointF
import android.graphics.RectF

object HandEyeCoverDetector {

    fun detect(
        handPoints: List<PointF>,
        leftEyeRect: RectF?,
        rightEyeRect: RectF?
    ): HandCoverResult {
        if (handPoints.isEmpty()) {
            return HandCoverResult(
                rightEyeCovered = false,
                leftEyeCovered = false,
                detectedHandCount = 0,
                reason = "손이 검출되지 않았습니다."
            )
        }

        val expandedLeft = leftEyeRect?.let { expand(it, 1.8f, 2.2f) }
        val expandedRight = rightEyeRect?.let { expand(it, 1.8f, 2.2f) }

        var leftHitCount = 0
        var rightHitCount = 0

        for (p in handPoints) {
            if (expandedLeft?.contains(p.x, p.y) == true) {
                leftHitCount++
            }

            if (expandedRight?.contains(p.x, p.y) == true) {
                rightHitCount++
            }
        }

        val leftCovered = leftHitCount >= 2
        val rightCovered = rightHitCount >= 2

        val reason = when {
            rightCovered && leftCovered -> "손이 양쪽 눈 영역과 겹칩니다."
            rightCovered -> "손이 오른쪽 눈 영역과 겹칩니다."
            leftCovered -> "손이 왼쪽 눈 영역과 겹칩니다."
            else -> "손은 검출되었지만 눈 영역과 겹치지 않습니다."
        }

        return HandCoverResult(
            rightEyeCovered = rightCovered,
            leftEyeCovered = leftCovered,
            detectedHandCount = handPoints.size / 21,
            reason = reason
        )
    }

    private fun expand(
        rect: RectF,
        scaleX: Float,
        scaleY: Float
    ): RectF {
        val cx = rect.centerX()
        val cy = rect.centerY()

        val halfW = rect.width() * scaleX / 2f
        val halfH = rect.height() * scaleY / 2f

        return RectF(
            cx - halfW,
            cy - halfH,
            cx + halfW,
            cy + halfH
        )
    }
}