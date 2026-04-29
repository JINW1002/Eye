package com.example.eye

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

data class EyeRoiResult(
    val leftEyeRect: RectF? = null,
    val rightEyeRect: RectF? = null,
    val leftEyeBitmap: Bitmap? = null,
    val rightEyeBitmap: Bitmap? = null
)

object EyeRoiExtractor {

    fun extractEyeRois(
        bitmap: Bitmap,
        landmarks: List<PointF>
    ): EyeRoiResult {
        if (landmarks.isEmpty()) return EyeRoiResult()

        val leftEyePoints = FaceMeshIndices.LEFT_EYE.mapNotNull {
            landmarks.getOrNull(it)
        }

        val rightEyePoints = FaceMeshIndices.RIGHT_EYE.mapNotNull {
            landmarks.getOrNull(it)
        }

        val leftRect = makeExpandedEyeRect(
            points = leftEyePoints,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )

        val rightRect = makeExpandedEyeRect(
            points = rightEyePoints,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )

        val leftBitmap = cropBitmap(bitmap, leftRect)
        val rightBitmap = cropBitmap(bitmap, rightRect)

        return EyeRoiResult(
            leftEyeRect = leftRect,
            rightEyeRect = rightRect,
            leftEyeBitmap = leftBitmap,
            rightEyeBitmap = rightBitmap
        )
    }

    private fun makeExpandedEyeRect(
        points: List<PointF>,
        imageWidth: Int,
        imageHeight: Int
    ): RectF? {
        if (points.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (pt in points) {
            minX = min(minX, pt.x)
            minY = min(minY, pt.y)
            maxX = max(maxX, pt.x)
            maxY = max(maxY, pt.y)
        }

        val width = maxX - minX
        val height = maxY - minY

        if (width <= 1f || height <= 1f) return null

        val expandX = width * 0.65f
        val expandY = height * 1.30f

        val left = (minX - expandX).coerceIn(0f, imageWidth.toFloat() - 1f)
        val top = (minY - expandY).coerceIn(0f, imageHeight.toFloat() - 1f)
        val right = (maxX + expandX).coerceIn(1f, imageWidth.toFloat())
        val bottom = (maxY + expandY).coerceIn(1f, imageHeight.toFloat())

        if (right <= left || bottom <= top) return null

        return RectF(left, top, right, bottom)
    }

    private fun cropBitmap(
        bitmap: Bitmap,
        rectF: RectF?
    ): Bitmap? {
        if (rectF == null) return null

        val rect = Rect(
            rectF.left.toInt().coerceIn(0, bitmap.width - 1),
            rectF.top.toInt().coerceIn(0, bitmap.height - 1),
            rectF.right.toInt().coerceIn(1, bitmap.width),
            rectF.bottom.toInt().coerceIn(1, bitmap.height)
        )

        val cropWidth = rect.width()
        val cropHeight = rect.height()

        if (cropWidth <= 1 || cropHeight <= 1) return null

        return Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            cropWidth,
            cropHeight
        )
    }
}