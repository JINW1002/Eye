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
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val landmarkPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val leftEyePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val rightEyePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val irisPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var landmarks: List<PointF> = emptyList()
    private var faceBox: RectF? = null
    private var leftEyePoints: List<PointF> = emptyList()
    private var rightEyePoints: List<PointF> = emptyList()
    private var leftIrisPoints: List<PointF> = emptyList()
    private var rightIrisPoints: List<PointF> = emptyList()
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
        imageWidth: Int,
        imageHeight: Int,
        faceDetected: Boolean
    ) {
        this.landmarks = landmarks
        this.faceBox = faceBox
        this.leftEyePoints = leftEyePoints
        this.rightEyePoints = rightEyePoints
        this.leftIrisPoints = leftIrisPoints
        this.rightIrisPoints = rightIrisPoints
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

        for (pt in landmarks) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 2.5f, landmarkPaint)
        }

        for (pt in leftEyePoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 4f, leftEyePaint)
        }

        for (pt in rightEyePoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 4f, rightEyePaint)
        }

        for (pt in leftIrisPoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 4f, irisPaint)
        }

        for (pt in rightIrisPoints) {
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 4f, irisPaint)
        }
    }
}