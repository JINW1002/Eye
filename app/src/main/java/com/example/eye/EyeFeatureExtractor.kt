package com.example.eye

import android.graphics.PointF

object EyeFeatureExtractor {

    fun extract(
        leftEyePoints: List<PointF>,
        rightEyePoints: List<PointF>,
        leftIrisPoints: List<PointF>,
        rightIrisPoints: List<PointF>,
        leftEyeRoiScore: Float,
        rightEyeRoiScore: Float
    ): EyeFeatureResult {

        val leftEyeCenter = centroid(leftEyePoints)
        val rightEyeCenter = centroid(rightEyePoints)

        val leftIrisCenter = if (leftIrisPoints.size >= 4) centroid(leftIrisPoints) else null
        val rightIrisCenter = if (rightIrisPoints.size >= 4) centroid(rightIrisPoints) else null

        val leftEyeOpenRatio = FaceMathUtils.eyeOpenRatio(leftEyePoints)
        val rightEyeOpenRatio = FaceMathUtils.eyeOpenRatio(rightEyePoints)

        val leftFeature = SingleEyeFeature(
            eyeCenter = leftEyeCenter,
            irisCenter = leftIrisCenter,
            irisVisible = leftIrisCenter != null,
            eyeOpenRatio = leftEyeOpenRatio,
            qualityScore = leftEyeRoiScore
        )

        val rightFeature = SingleEyeFeature(
            eyeCenter = rightEyeCenter,
            irisCenter = rightIrisCenter,
            irisVisible = rightIrisCenter != null,
            eyeOpenRatio = rightEyeOpenRatio,
            qualityScore = rightEyeRoiScore
        )

        return EyeFeatureResult(
            left = leftFeature,
            right = rightFeature,
            bothEyesReady = leftFeature.qualityScore >= 0.45f &&
                    rightFeature.qualityScore >= 0.45f &&
                    leftFeature.irisVisible &&
                    rightFeature.irisVisible
        )
    }

    private fun centroid(points: List<PointF>): PointF? {
        if (points.isEmpty()) return null

        var sumX = 0f
        var sumY = 0f

        for (pt in points) {
            sumX += pt.x
            sumY += pt.y
        }

        return PointF(sumX / points.size, sumY / points.size)
    }
}