package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    private val context: Context
) {
    private var handLandmarker: HandLandmarker? = null

    fun setup() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.35f)
            .setMinHandPresenceConfidence(0.35f)
            .setMinTrackingConfidence(0.35f)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(bitmap: Bitmap): HandLandmarkerResult? {
        val landmarker = handLandmarker ?: return null
        val mpImage = BitmapImageBuilder(bitmap).build()
        return landmarker.detect(mpImage)
    }

    fun clear() {
        handLandmarker?.close()
        handLandmarker = null
    }

    companion object {
        fun handPointsToPixel(
            result: HandLandmarkerResult?,
            imageWidth: Int,
            imageHeight: Int
        ): List<PointF> {
            if (result == null || result.landmarks().isEmpty()) {
                return emptyList()
            }

            val points = mutableListOf<PointF>()

            for (hand in result.landmarks()) {
                for (lm in hand) {
                    points.add(
                        PointF(
                            lm.x() * imageWidth,
                            lm.y() * imageHeight
                        )
                    )
                }
            }

            return points
        }
    }
}