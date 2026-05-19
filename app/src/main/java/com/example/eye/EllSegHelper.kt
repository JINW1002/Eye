package com.example.eye

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp

class EllSegHelper(private val context: Context) {

    private var module: Module? = null

    private val inputWidth = 240
    private val inputHeight = 160

    fun setup() {
        val modelPath = assetFilePath("ellseg_iris_visibility.ptl")
        module = Module.load(modelPath)
    }

    fun analyzeEye(eyeBitmap: Bitmap?): EllSegVisibilityResult {
        val model = module ?: return EllSegVisibilityResult(
            available = false,
            reason = "EllSeg 모델이 로드되지 않았습니다."
        )

        if (eyeBitmap == null) {
            return EllSegVisibilityResult(
                available = false,
                reason = "Eye ROI가 없습니다."
            )
        }

        val resized = Bitmap.createScaledBitmap(
            eyeBitmap,
            inputWidth,
            inputHeight,
            true
        )

        val inputTensor = bitmapToTensor(resized)

        val output = model.forward(IValue.from(inputTensor)).toTensor()
        val scores = output.dataAsFloatArray

        /*
         * 전제:
         * 모델 출력 shape = [1, 2, H, W]
         * channel 0 = background / non-iris
         * channel 1 = iris probability
         *
         * 만약 네가 export한 EllSeg output 구조가 다르면
         * 여기 parse 부분만 바꾸면 됨.
         */

        val pixelCount = inputWidth * inputHeight
        if (scores.size < pixelCount * 2) {
            return EllSegVisibilityResult(
                available = false,
                reason = "EllSeg 출력 shape가 예상과 다릅니다."
            )
        }

        var irisPixels = 0
        var confidenceSum = 0f

        for (i in 0 until pixelCount) {
            val bgLogit = scores[i]
            val irisLogit = scores[pixelCount + i]

            val irisProb = softmax2(bgLogit, irisLogit)

            if (irisProb > 0.50f) {
                irisPixels++
                confidenceSum += irisProb
            }
        }

        val irisAreaRatio = irisPixels.toFloat() / pixelCount.toFloat()
        val confidence = if (irisPixels > 0) confidenceSum / irisPixels else 0f

        val irisVisible =
            irisAreaRatio >= 0.025f &&
                    confidence >= 0.55f

        val reason = when {
            irisVisible -> "EllSeg 기준 홍채가 보입니다."
            irisAreaRatio < 0.025f -> "EllSeg 기준 홍채 영역이 거의 없습니다."
            else -> "EllSeg 기준 홍채 confidence가 낮습니다."
        }

        return EllSegVisibilityResult(
            available = true,
            irisVisible = irisVisible,
            irisAreaRatio = irisAreaRatio,
            confidence = confidence,
            reason = reason
        )
    }

    fun clear() {
        module = null
    }

    private fun bitmapToTensor(bitmap: Bitmap): Tensor {
        val floats = FloatArray(1 * 3 * inputHeight * inputWidth)

        var indexR = 0
        var indexG = inputHeight * inputWidth
        var indexB = inputHeight * inputWidth * 2

        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = bitmap.getPixel(x, y)

                val r = ((pixel shr 16) and 0xff) / 255f
                val g = ((pixel shr 8) and 0xff) / 255f
                val b = (pixel and 0xff) / 255f

                floats[indexR++] = (r - 0.5f) / 0.5f
                floats[indexG++] = (g - 0.5f) / 0.5f
                floats[indexB++] = (b - 0.5f) / 0.5f
            }
        }

        return Tensor.fromBlob(
            floats,
            longArrayOf(1, 3, inputHeight.toLong(), inputWidth.toLong())
        )
    }

    private fun softmax2(bg: Float, iris: Float): Float {
        val eBg = exp(bg)
        val eIris = exp(iris)
        return eIris / (eBg + eIris)
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(context.filesDir, assetName)

        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }

                output.flush()
            }
        }

        return file.absolutePath
    }
}