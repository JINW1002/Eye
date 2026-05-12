package com.example.eye

import android.graphics.Bitmap
import kotlin.math.abs

data class EyeOcclusionResult(
    val covered: Boolean = false,
    val score: Int = 0,
    val reason: String = "가림 판단 전",
    val brightness: Float = 0f,
    val texture: Float = 0f
)

object EyeOcclusionDetector {

    fun detect(
        eyeBitmap: Bitmap?,
        irisVisible: Boolean,
        eyeOpenRatio: Float,
        eyeRoiValid: Boolean,
        eyeRoiScore: Float,
        otherEyeRoiScore: Float
    ): EyeOcclusionResult {
        var score = 0
        val reasons = mutableListOf<String>()

        val imageStats = analyzeBitmap(eyeBitmap)

        if (!irisVisible) {
            score += 2
            reasons.add("홍채 미검출")
        }

        if (eyeOpenRatio < 0.18f) {
            score += 1
            reasons.add("눈 뜸 비율 낮음")
        }

        if (!eyeRoiValid) {
            score += 2
            reasons.add("ROI 무효")
        }

        if (eyeRoiScore < 0.45f) {
            score += 1
            reasons.add("ROI 품질 낮음")
        }

        if (otherEyeRoiScore - eyeRoiScore > 0.25f) {
            score += 1
            reasons.add("반대쪽 눈 대비 품질 급락")
        }

        if (imageStats != null) {
            if (imageStats.brightness < 35f) {
                score += 1
                reasons.add("ROI가 너무 어두움")
            }

            if (imageStats.texture < 8f) {
                score += 1
                reasons.add("눈 텍스처 부족")
            }
        }

        val covered = score >= 3

        return EyeOcclusionResult(
            covered = covered,
            score = score,
            reason = if (reasons.isEmpty()) "가림 아님" else reasons.joinToString(" / "),
            brightness = imageStats?.brightness ?: 0f,
            texture = imageStats?.texture ?: 0f
        )
    }

    private data class ImageStats(
        val brightness: Float,
        val texture: Float
    )

    private fun analyzeBitmap(bitmap: Bitmap?): ImageStats? {
        if (bitmap == null || bitmap.width < 4 || bitmap.height < 4) return null

        var brightnessSum = 0.0
        var textureSum = 0.0
        var count = 0

        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)

        var y = 1
        while (y < bitmap.height - 1) {
            var x = 1
            while (x < bitmap.width - 1) {
                val c = gray(bitmap.getPixel(x, y))
                val l = gray(bitmap.getPixel(x - 1, y))
                val r = gray(bitmap.getPixel(x + 1, y))
                val t = gray(bitmap.getPixel(x, y - 1))
                val b = gray(bitmap.getPixel(x, y + 1))

                brightnessSum += c
                textureSum += abs(4 * c - l - r - t - b)
                count++

                x += stepX
            }
            y += stepY
        }

        if (count == 0) return null

        return ImageStats(
            brightness = (brightnessSum / count).toFloat(),
            texture = (textureSum / count).toFloat()
        )
    }

    private fun gray(pixel: Int): Int {
        val red = (pixel shr 16) and 0xff
        val green = (pixel shr 8) and 0xff
        val blue = pixel and 0xff
        return (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
    }
}