package com.example.eye

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.abs

data class EyeRoiQualityResult(
    val leftScore: Float = 0f,
    val rightScore: Float = 0f,
    val sizeScore: Float = 0f,
    val symmetryScore: Float = 0f,
    val blurScoreLeft: Float = 0f,
    val blurScoreRight: Float = 0f,
    val leftValid: Boolean = false,
    val rightValid: Boolean = false,
    val bothValid: Boolean = false,
    val reason: String = "ROI 없음"
)

object EyeRoiQualityEvaluator {

    private const val MIN_ROI_WIDTH = 60f
    private const val MIN_ROI_HEIGHT = 40f
    private const val MIN_FINAL_SCORE = 0.45f

    fun evaluate(
        imageWidth: Int,
        imageHeight: Int,
        leftRect: RectF?,
        rightRect: RectF?,
        leftBitmap: Bitmap?,
        rightBitmap: Bitmap?
    ): EyeRoiQualityResult {
        if (leftRect == null || rightRect == null || leftBitmap == null || rightBitmap == null) {
            return EyeRoiQualityResult(reason = "좌우 눈 ROI가 아직 충분하지 않습니다.")
        }

        val leftSizeScore = calcSizeScore(leftRect, imageWidth, imageHeight)
        val rightSizeScore = calcSizeScore(rightRect, imageWidth, imageHeight)
        val sizeScore = (leftSizeScore + rightSizeScore) / 2f

        val symmetryScore = calcSymmetryScore(leftRect, rightRect)

        val blurLeft = calcSharpnessScore(leftBitmap)
        val blurRight = calcSharpnessScore(rightBitmap)

        val leftFinal = combine(leftSizeScore, symmetryScore, blurLeft)
        val rightFinal = combine(rightSizeScore, symmetryScore, blurRight)

        val leftValid = leftFinal >= MIN_FINAL_SCORE
        val rightValid = rightFinal >= MIN_FINAL_SCORE
        val bothValid = leftValid && rightValid

        val reason = when {
            !leftValid && !rightValid -> "양쪽 눈 ROI 품질이 낮습니다."
            !leftValid -> "왼쪽 눈 ROI 품질이 낮습니다."
            !rightValid -> "오른쪽 눈 ROI 품질이 낮습니다."
            else -> "양쪽 눈 ROI 품질이 충분합니다."
        }

        return EyeRoiQualityResult(
            leftScore = leftFinal,
            rightScore = rightFinal,
            sizeScore = sizeScore,
            symmetryScore = symmetryScore,
            blurScoreLeft = blurLeft,
            blurScoreRight = blurRight,
            leftValid = leftValid,
            rightValid = rightValid,
            bothValid = bothValid,
            reason = reason
        )
    }

    private fun combine(
        sizeScore: Float,
        symmetryScore: Float,
        blurScore: Float
    ): Float {
        return (
                sizeScore * 0.45f +
                        symmetryScore * 0.20f +
                        blurScore * 0.35f
                ).coerceIn(0f, 1f)
    }

    private fun calcSizeScore(
        rect: RectF,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val width = rect.width()
        val height = rect.height()

        val widthScore = (width / MIN_ROI_WIDTH).coerceIn(0f, 1f)
        val heightScore = (height / MIN_ROI_HEIGHT).coerceIn(0f, 1f)

        val normalizedArea = (width * height) / (imageWidth.toFloat() * imageHeight.toFloat())
        val areaScore = (normalizedArea / 0.015f).coerceIn(0f, 1f)

        return ((widthScore + heightScore + areaScore) / 3f).coerceIn(0f, 1f)
    }

    private fun calcSymmetryScore(
        leftRect: RectF,
        rightRect: RectF
    ): Float {
        val leftArea = leftRect.width() * leftRect.height()
        val rightArea = rightRect.width() * rightRect.height()

        if (leftArea <= 0f || rightArea <= 0f) return 0f

        val ratio = if (leftArea > rightArea) {
            rightArea / leftArea
        } else {
            leftArea / rightArea
        }

        return ratio.coerceIn(0f, 1f)
    }

    private fun calcSharpnessScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        if (width < 3 || height < 3) return 0f

        var sum = 0.0
        var count = 0

        for (y in 1 until height - 1 step 2) {
            for (x in 1 until width - 1 step 2) {
                val c = gray(bitmap.getPixel(x, y))
                val left = gray(bitmap.getPixel(x - 1, y))
                val right = gray(bitmap.getPixel(x + 1, y))
                val top = gray(bitmap.getPixel(x, y - 1))
                val bottom = gray(bitmap.getPixel(x, y + 1))

                val lap = abs((4 * c - left - right - top - bottom).toDouble())
                sum += lap
                count++
            }
        }

        if (count == 0) return 0f

        val avg = (sum / count).toFloat()

        return (avg / 40f).coerceIn(0f, 1f)
    }

    private fun gray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}