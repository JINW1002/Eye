package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class EyeResult(
    val isCovered: Boolean,
    val confidence: Float,
    val debug: String,
    val available: Boolean
)

class EyeClassifierHelper(
    private val context: Context
) {

    private var interpreter: Interpreter? = null

    // 🔥 1️⃣ setup() 여기 있음
    fun setup() {
        try {
            val modelBytes = context.assets.open("eye_classifier.tflite").readBytes()

            val buffer = ByteBuffer.allocateDirect(modelBytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(modelBytes)
            buffer.rewind()

            interpreter = Interpreter(buffer)

        } catch (e: Exception) {
            e.printStackTrace()
            interpreter = null
        }
    }

    fun classify(bitmap: Bitmap?): EyeResult {

        // 🔥 2️⃣ 여기 if(interpreter == null) 들어가는 위치
        if (interpreter == null) {
            return EyeResult(
                isCovered = false,
                confidence = 0f,
                debug = "no model",
                available = false
            )
        }

        if (bitmap == null) {
            return EyeResult(
                isCovered = false,
                confidence = 0f,
                debug = "no image",
                available = false
            )
        }

        // 🔥 입력 크기 (학습할 때 맞출 예정)
        val inputSize = 224

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val input = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        input.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            input.putFloat(((pixel shr 16 and 0xFF) / 255f))
            input.putFloat(((pixel shr 8 and 0xFF) / 255f))
            input.putFloat(((pixel and 0xFF) / 255f))
        }

        val output = Array(1) { FloatArray(2) } // [visible, covered]

        interpreter?.run(input, output)

        val visible = output[0][0]
        val covered = output[0][1]

        val isCovered = covered > 0.5f

        return EyeResult(
            isCovered = isCovered,
            confidence = covered,
            debug = "visible=$visible covered=$covered",
            available = true
        )
    }
}