package com.example.eye

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object HirschbergAnalyzer {

    fun analyze(
        leftBitmap: Bitmap?,
        rightBitmap: Bitmap?
    ): HirschbergResult {
        if (leftBitmap == null || rightBitmap == null) {
            return HirschbergResult(
                valid = false,
                reason = "좌우 눈 ROI 이미지가 없어 반사광 검사를 할 수 없습니다."
            )
        }

        val left = analyzeSingleEye(leftBitmap)
        val right = analyzeSingleEye(rightBitmap)

        if (!left.valid || !right.valid) {
            return HirschbergResult(
                valid = false,
                left = left,
                right = right,
                reason = "홍채 경계 또는 각막 반사광 검출에 실패했습니다."
            )
        }

        val horizontalDiff = abs(left.dlrX - right.dlrX)
        val verticalDiff = abs(left.dlrY - right.dlrY)

        val score = (
                (horizontalDiff / 0.20f).coerceIn(0f, 1f) * 0.8f +
                        (verticalDiff / 0.15f).coerceIn(0f, 1f) * 0.2f
                ).coerceIn(0f, 1f)

        val suspected = score >= 0.45f

        val reason = when {
            !suspected -> "홍채 중심과 반사광 위치 차이가 좌우에서 비교적 안정적입니다."
            horizontalDiff >= verticalDiff -> "좌우 눈의 수평 반사광 위치 차이가 큽니다."
            else -> "좌우 눈의 수직 반사광 위치 차이가 큽니다."
        }

        return HirschbergResult(
            valid = true,
            left = left,
            right = right,
            horizontalDiff = horizontalDiff,
            verticalDiff = verticalDiff,
            score = score,
            suspected = suspected,
            reason = reason
        )
    }

    private fun analyzeSingleEye(bitmap: Bitmap): SingleEyeHirschbergResult {
        val iris = detectIrisCenter(bitmap)
        val reflection = detectCornealReflection(bitmap)

        if (iris == null) {
            return SingleEyeHirschbergResult(
                valid = false,
                reason = "홍채 중심을 찾지 못했습니다."
            )
        }

        if (reflection == null) {
            return SingleEyeHirschbergResult(
                valid = false,
                reason = "각막 반사광을 찾지 못했습니다."
            )
        }

        val dlrX = reflection.x - iris.x
        val dlrY = reflection.y - iris.y

        return SingleEyeHirschbergResult(
            valid = true,
            irisCenterX = iris.x,
            irisCenterY = iris.y,
            reflectionX = reflection.x,
            reflectionY = reflection.y,
            dlrX = dlrX,
            dlrY = dlrY,
            reason = "홍채 중심과 각막 반사광을 검출했습니다."
        )
    }

    private fun detectIrisCenter(bitmap: Bitmap): NormalizedPoint? {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 30 || height < 20) return null

        var sumBrightness = 0.0
        var countBrightness = 0

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                sumBrightness += brightness(bitmap.getPixel(x, y))
                countBrightness++
            }
        }

        if (countBrightness == 0) return null

        val avgBrightness = sumBrightness / countBrightness
        val threshold = (avgBrightness * 0.72).toInt().coerceIn(30, 115)

        var sumX = 0.0
        var sumY = 0.0
        var count = 0

        val minY = (height * 0.15f).toInt()
        val maxY = (height * 0.85f).toInt()

        for (y in minY until maxY) {
            for (x in 0 until width) {
                val b = brightness(bitmap.getPixel(x, y))

                if (b < threshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        if (count < width * height * 0.01f) return null

        return NormalizedPoint(
            x = (sumX / count).toFloat() / width.toFloat(),
            y = (sumY / count).toFloat() / height.toFloat()
        )
    }

    private fun detectCornealReflection(bitmap: Bitmap): NormalizedPoint? {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 30 || height < 20) return null

        // 눈꺼풀, 피부, 눈물 라인 반사를 줄이기 위해 중앙 영역만 탐색
        val minX = (width * 0.15f).toInt()
        val maxX = (width * 0.85f).toInt()
        val minY = (height * 0.20f).toInt()
        val maxY = (height * 0.80f).toInt()

        var maxBrightness = 0

        for (y in minY until maxY step 2) {
            for (x in minX until maxX step 2) {
                val b = brightness(bitmap.getPixel(x, y))
                if (b > maxBrightness) {
                    maxBrightness = b
                }
            }
        }

        // 기존 180보다 낮춰 약한 반사광도 잡히게 함
        if (maxBrightness < 160) return null

        // 기존 0.88보다 낮춰 후보 픽셀을 조금 더 넓게 포함
        val threshold = (maxBrightness * 0.82f).toInt()

        var sumX = 0.0
        var sumY = 0.0
        var count = 0

        for (y in minY until maxY) {
            for (x in minX until maxX) {
                val b = brightness(bitmap.getPixel(x, y))

                if (b >= threshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        if (count < 2) return null

        return NormalizedPoint(
            x = (sumX / count).toFloat() / width.toFloat(),
            y = (sumY / count).toFloat() / height.toFloat()
        )
    }

    private fun brightness(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return ((r + g + b) / 3f).toInt()
    }

    private data class NormalizedPoint(
        val x: Float,
        val y: Float
    )
}