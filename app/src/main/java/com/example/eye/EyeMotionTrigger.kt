package com.example.eye

import android.graphics.Bitmap
import kotlin.math.abs

object EyeMotionTrigger {

    fun detectCoverMotion(
        previous: Bitmap?,
        current: Bitmap?
    ): Boolean {

        if (previous == null || current == null) {
            return false
        }

        val width = minOf(previous.width, current.width)
        val height = minOf(previous.height, current.height)

        if (width < 10 || height < 10) {
            return false
        }

        var diffSum = 0.0
        var count = 0

        val stepX = (width / 20).coerceAtLeast(1)
        val stepY = (height / 20).coerceAtLeast(1)

        var y = 0
        while (y < height) {

            var x = 0

            while (x < width) {

                val p1 = gray(previous.getPixel(x, y))
                val p2 = gray(current.getPixel(x, y))

                diffSum += abs(p1 - p2)

                count++

                x += stepX
            }

            y += stepY
        }

        val averageDiff = diffSum / count.toDouble()

        return averageDiff > 28.0
    }

    private fun gray(pixel: Int): Int {

        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff

        return ((r * 0.299) +
                (g * 0.587) +
                (b * 0.114)).toInt()
    }
}