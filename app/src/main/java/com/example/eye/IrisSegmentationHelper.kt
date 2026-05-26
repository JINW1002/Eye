package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.exp

class IrisSegmentationHelper(
    context: Context
) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val inputWidth = 640
    private val inputHeight = 480

    private val irisClassIndex = 1
    private val pupilClassIndex = 2

    init {
        val modelBytes = context.assets
            .open("iris_semseg_upp_scse_mobilenetv2.onnx")
            .readBytes()

        session = env.createSession(
            modelBytes,
            OrtSession.SessionOptions()
        )
    }

    fun analyzeEye(eyeBitmap: Bitmap?): IrisVisibilityResult {
        if (eyeBitmap == null) {
            return IrisVisibilityResult(
                available = false,
                reason = "Eye ROI가 없습니다."
            )
        }

        return try {
            val inputName = session.inputNames.iterator().next()
            val resized = Bitmap.createScaledBitmap(
                eyeBitmap,
                inputWidth,
                inputHeight,
                true
            )

            val input = FloatArray(1 * 3 * inputHeight * inputWidth)

            var rIndex = 0
            var gIndex = inputHeight * inputWidth
            var bIndex = inputHeight * inputWidth * 2

            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixel = resized.getPixel(x, y)

                    val r = ((pixel shr 16) and 0xff) / 255f
                    val g = ((pixel shr 8) and 0xff) / 255f
                    val b = (pixel and 0xff) / 255f

                    input[rIndex++] = r
                    input[gIndex++] = g
                    input[bIndex++] = b
                }
            }

            val tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(input),
                longArrayOf(
                    1,
                    3,
                    inputHeight.toLong(),
                    inputWidth.toLong()
                )
            )

            session.run(mapOf(inputName to tensor)).use { result ->
                tensor.close()

                val output = result[0].value
                parseOutput(output)
            }
        } catch (e: Exception) {
            IrisVisibilityResult(
                available = false,
                reason = "ONNX 실행 실패: ${e.message}"
            )
        }
    }

    private fun parseOutput(output: Any): IrisVisibilityResult {
        val array4d = output as? Array<Array<Array<FloatArray>>>
            ?: return IrisVisibilityResult(
                available = false,
                reason = "ONNX 출력 형식을 해석할 수 없습니다."
            )

        val batch0 = array4d[0]
        val channels = batch0.size
        val height = batch0[0].size
        val width = batch0[0][0].size

        if (channels <= pupilClassIndex) {
            return IrisVisibilityResult(
                available = false,
                reason = "출력 채널 수가 예상보다 적습니다. channels=$channels"
            )
        }

        var irisPixels = 0
        var pupilPixels = 0
        val totalPixels = height * width

        for (y in 0 until height) {
            for (x in 0 until width) {
                var bestClass = 0
                var bestValue = batch0[0][y][x]

                for (c in 1 until channels) {
                    val value = batch0[c][y][x]
                    if (value > bestValue) {
                        bestValue = value
                        bestClass = c
                    }
                }

                if (bestClass == irisClassIndex) irisPixels++
                if (bestClass == pupilClassIndex) pupilPixels++
            }
        }

        val irisAreaRatio = irisPixels.toFloat() / totalPixels.toFloat()
        val pupilAreaRatio = pupilPixels.toFloat() / totalPixels.toFloat()

        val irisVisible =
            irisAreaRatio >= 0.015f ||
                    pupilAreaRatio >= 0.003f

        val reason = when {
            irisVisible -> "ONNX segmentation 기준 홍채/동공 영역이 보입니다."
            else -> "ONNX segmentation 기준 홍채/동공 영역이 거의 없습니다."
        }

        return IrisVisibilityResult(
            available = true,
            irisVisible = irisVisible,
            irisAreaRatio = irisAreaRatio,
            pupilAreaRatio = pupilAreaRatio,
            reason = reason
        )
    }

    fun close() {
        session.close()
    }
}