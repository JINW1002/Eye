package com.example.eye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val guidePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val successPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private var leftEyeGuideRect: RectF? = null
    private var rightEyeGuideRect: RectF? = null

    private var bothEyesReady: Boolean = false

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    fun setResults(
        leftEyeRoiRect: RectF?,
        rightEyeRoiRect: RectF?,
        bothEyesReady: Boolean,
        imageWidth: Int,
        imageHeight: Int
    ) {
        this.leftEyeGuideRect = leftEyeRoiRect
        this.rightEyeGuideRect = rightEyeRoiRect
        this.bothEyesReady = bothEyesReady
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()

        val paint = if (bothEyesReady) {
            successPaint
        } else {
            guidePaint
        }

        leftEyeGuideRect?.let { rect ->
            canvas.drawRoundRect(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY,
                20f,
                20f,
                paint
            )
        }

        rightEyeGuideRect?.let { rect ->
            canvas.drawRoundRect(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY,
                20f,
                20f,
                paint
            )
        }
    }
}