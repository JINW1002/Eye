package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.ByteArrayOutputStream

class FaceLandmarkerHelper(
    private val context: Context
) {
    private var faceLandmarker: FaceLandmarker? = null

    fun setup() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setOutputFaceBlendshapes(false)
            .setOutputFacialTransformationMatrixes(false)
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    fun clear() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    fun detect(bitmap: Bitmap): FaceLandmarkerResult? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        return faceLandmarker?.detect(mpImage)
    }

    companion object {
        fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, imageProxy.width, imageProxy.height),
                95,
                out
            )

            val imageBytes = out.toByteArray()
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }

        fun rotateBitmap(
            bitmap: Bitmap,
            rotationDegrees: Int,
            mirrorFrontCamera: Boolean
        ): Bitmap {
            val matrix = Matrix()

            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }

            if (mirrorFrontCamera) {
                matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }

            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

        fun normalizedToPixelPoint(
            x: Float,
            y: Float,
            imageWidth: Int,
            imageHeight: Int
        ): PointF {
            return PointF(x * imageWidth, y * imageHeight)
        }
    }
}