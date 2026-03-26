package com.example.eye

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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

    fun eyeHorizontalVector(outer: PointF, inner: PointF): PointF {
        return PointF(inner.x - outer.x, inner.y - outer.y)
    }

    fun irisOffsetRatio(outer: PointF, inner: PointF, irisCenter: PointF): Float {
        val dx = inner.x - outer.x
        val dy = inner.y - outer.y
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 1e-6f) return 0f

        val px = irisCenter.x - outer.x
        val py = irisCenter.y - outer.y

        val proj = (px * dx + py * dy) / lengthSq
        return proj
    }

    fun vectorAngleDeg(v1: PointF, v2: PointF): Float? {
        val n1 = sqrt(v1.x * v1.x + v1.y * v1.y)
        val n2 = sqrt(v2.x * v2.x + v2.y * v2.y)
        if (n1 < 1e-6f || n2 < 1e-6f) return null

        val dot = v1.x * v2.x + v1.y * v2.y
        val cos = (dot / (n1 * n2)).coerceIn(-1f, 1f)
        return Math.toDegrees(kotlin.math.acos(cos).toDouble()).toFloat()
    }

    fun isFaceCentered(faceBox: RectF, imageWidth: Int, imageHeight: Int): Boolean {
        val centerX = faceBox.centerX()
        val centerY = faceBox.centerY()

        val normX = centerX / imageWidth
        val normY = centerY / imageHeight

        return abs(normX - 0.5f) < 0.18f && abs(normY - 0.45f) < 0.20f
    }
}