package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.abs

object FaceMathUtils {

    fun distance(a: PointF, b: PointF): Float {
        return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }

    fun center(points: List<PointF>): PointF {
        if (points.isEmpty()) return PointF(0f, 0f)
        val x = points.sumOf { it.x.toDouble() }.toFloat() / points.size
        val y = points.sumOf { it.y.toDouble() }.toFloat() / points.size
        return PointF(x, y)
    }

    fun boundingBox(points: List<PointF>): RectF? {
        if (points.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = 0f
        var maxY = 0f

        for (pt in points) {
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    fun eyeOpenRatio(points: List<PointF>): Float {
        if (points.size < 8) return 0f

        val left = points[0]
        val right = points[1]
        val top1 = points[2]
        val bottom1 = points[3]
        val top2 = points[4]
        val bottom2 = points[5]

        val vertical1 = distance(top1, bottom1)
        val vertical2 = distance(top2, bottom2)
        val horizontal = distance(left, right).coerceAtLeast(1e-6f)

        return ((vertical1 + vertical2) / 2f) / horizontal
    }

    fun irisOffsetRatio(outer: PointF, inner: PointF, irisCenter: PointF): Float {
        val dx = inner.x - outer.x
        val dy = inner.y - outer.y
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 1e-6f) return 0f

        val px = irisCenter.x - outer.x
        val py = irisCenter.y - outer.y

        return (px * dx + py * dy) / lengthSq
    }

    fun isFaceCentered(faceBox: RectF, imageWidth: Int, imageHeight: Int): Boolean {
        val centerX = faceBox.centerX()
        val centerY = faceBox.centerY()

        val normX = centerX / imageWidth
        val normY = centerY / imageHeight

        return abs(normX - 0.5f) < 0.18f && abs(normY - 0.45f) < 0.20f
    }
}