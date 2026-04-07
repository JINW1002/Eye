package com.example.eye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val facePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val leftEyeRoiPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val rightEyeRoiPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val irisPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var faceBox: RectF? = null
    private var leftIrisPoints: List<PointF> = emptyList()
    private var rightIrisPoints: List<PointF> = emptyList()
    private var leftEyeRoiRect: RectF? = null
    private var rightEyeRoiRect: RectF? = null

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var faceDetected: Boolean = false

    fun setResults(
        landmarks: List<PointF>,
        faceBox: RectF?,
        leftEyePoints: List<PointF>,
        rightEyePoints: List<PointF>,
        leftIrisPoints: List<PointF>,
        rightIrisPoints: List<PointF>,
        leftEyeRoiRect: RectF?,
        rightEyeRoiRect: RectF?,
        imageWidth: Int,
        imageHeight: Int,
        faceDetected: Boolean
    ) {
        this.faceBox = faceBox
        this.leftIrisPoints = leftIrisPoints
        this.rightIrisPoints = rightIrisPoints
        this.leftEyeRoiRect = leftEyeRoiRect
        this.rightEyeRoiRect = rightEyeRoiRect
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.faceDetected = faceDetected
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!faceDetected || imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()

        faceBox?.let { box ->
            canvas.drawRect(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY,
                facePaint
            )
        }

        leftEyeRoiRect?.let { rect ->
            canvas.drawRect(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY,
                leftEyeRoiPaint
            )
        }

        rightEyeRoiRect?.let { rect ->
            canvas.drawRect(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY,
                rightEyeRoiPaint
            )
        }

        for (pt in leftIrisPoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 5f, irisPaint)
        }

        for (pt in rightIrisPoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 5f, irisPaint)
        }
    }
}