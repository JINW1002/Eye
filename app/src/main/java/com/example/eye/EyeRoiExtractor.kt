package com.example.eye

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.roundToInt

data class EyeRoiResult(
    val leftEyeRect: RectF? = null,
    val rightEyeRect: RectF? = null,
    val leftEyeBitmap: Bitmap? = null,
    val rightEyeBitmap: Bitmap? = null,
    val leftEyeValid: Boolean = false,
    val rightEyeValid: Boolean = false
)

object EyeRoiExtractor {

    fun extractEyeRois(
        bitmap: Bitmap,
        landmarks: List<PointF>
    ): EyeRoiResult {
        if (landmarks.isEmpty()) {
            return EyeRoiResult()
        }

        val leftEyePoints = safePoints(landmarks, FaceMeshIndices.LEFT_EYE)
        val rightEyePoints = safePoints(landmarks, FaceMeshIndices.RIGHT_EYE)

        val leftBaseRect = FaceMathUtils.boundingBox(leftEyePoints)
        val rightBaseRect = FaceMathUtils.boundingBox(rightEyePoints)

        val leftExpandedRect = leftBaseRect?.let {
            expandRect(
                rect = it,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                scaleX = 1.8f,
                scaleY = 2.2f
            )
        }

        val rightExpandedRect = rightBaseRect?.let {
            expandRect(
                rect = it,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                scaleX = 1.8f,
                scaleY = 2.2f
            )
        }

        val leftBitmap = leftExpandedRect?.let { cropBitmap(bitmap, it) }
        val rightBitmap = rightExpandedRect?.let { cropBitmap(bitmap, it) }

        return EyeRoiResult(
            leftEyeRect = leftExpandedRect,
            rightEyeRect = rightExpandedRect,
            leftEyeBitmap = leftBitmap,
            rightEyeBitmap = rightBitmap,
            leftEyeValid = leftExpandedRect != null && leftBitmap != null,
            rightEyeValid = rightExpandedRect != null && rightBitmap != null
        )
    }

    private fun safePoints(
        landmarks: List<PointF>,
        indices: List<Int>
    ): List<PointF> {
        return indices.mapNotNull { index ->
            landmarks.getOrNull(index)
        }
    }

    private fun expandRect(
        rect: RectF,
        imageWidth: Int,
        imageHeight: Int,
        scaleX: Float,
        scaleY: Float
    ): RectF {
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val halfWidth = rect.width() * scaleX / 2f
        val halfHeight = rect.height() * scaleY / 2f

        val left = (centerX - halfWidth).coerceAtLeast(0f)
        val top = (centerY - halfHeight).coerceAtLeast(0f)
        val right = (centerX + halfWidth).coerceAtMost(imageWidth.toFloat())
        val bottom = (centerY + halfHeight).coerceAtMost(imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }

    private fun cropBitmap(
        bitmap: Bitmap,
        rect: RectF
    ): Bitmap? {
        val left = rect.left.roundToInt().coerceIn(0, bitmap.width - 1)
        val top = rect.top.roundToInt().coerceIn(0, bitmap.height - 1)
        val right = rect.right.roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = rect.bottom.roundToInt().coerceIn(top + 1, bitmap.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        return try {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            null
        }
    }
}