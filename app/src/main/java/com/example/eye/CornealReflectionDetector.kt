package com.example.eye

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

data class CornealReflectionResult(
    val valid: Boolean = false,
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
    val horizontalDiff: Float = 0f,
    val verticalDiff: Float = 0f,
    val reason: String = "반사광 검출 전"
)

object CornealReflectionDetector {

    fun detect(
        leftBitmap: Bitmap?,
        rightBitmap: Bitmap?
    ): CornealReflectionResult {
        if (leftBitmap == null || rightBitmap == null) {
            return CornealReflectionResult(
                valid = false,
                reason = "좌우 눈 ROI 이미지가 없어 반사광을 찾을 수 없습니다."
            )
        }

        val leftSpot = findBrightSpot(leftBitmap)
        val rightSpot = findBrightSpot(rightBitmap)

        if (leftSpot == null || rightSpot == null) {
            return CornealReflectionResult(
                valid = false,
                reason = "좌우 눈에서 반사광 밝은 점을 안정적으로 찾지 못했습니다."
            )
        }

        val horizontalDiff = abs(leftSpot.x - rightSpot.x)
        val verticalDiff = abs(leftSpot.y - rightSpot.y)

        return CornealReflectionResult(
            valid = true,
            leftX = leftSpot.x,
            leftY = leftSpot.y,
            rightX = rightSpot.x,
            rightY = rightSpot.y,
            horizontalDiff = horizontalDiff,
            verticalDiff = verticalDiff,
            reason = "좌우 눈 반사광 위치를 계산했습니다."
        )
    }

    private fun findBrightSpot(bitmap: Bitmap): NormalizedPoint? {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 10 || height < 10) return null

        var maxBrightness = 0

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                val brightness = brightness(bitmap.getPixel(x, y))
                if (brightness > maxBrightness) {
                    maxBrightness = brightness
                }
            }
        }

        if (maxBrightness < 180) return null

        val threshold = (maxBrightness * 0.88f).toInt()

        var sumX = 0f
        var sumY = 0f
        var count = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val brightness = brightness(bitmap.getPixel(x, y))

                if (brightness >= threshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        if (count < 3) return null

        return NormalizedPoint(
            x = (sumX / count) / width.toFloat(),
            y = (sumY / count) / height.toFloat()
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