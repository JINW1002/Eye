package com.example.eye

import android.content.Context
import android.graphics.Bitmap

class FrameAnalyzer(
    private val context: Context
) {

    private val eyeClassifier = EyeClassifierHelper(context)
    private val saver = EyeDatasetSaver(context)

    var currentLabel = "visible_eye"

    init {
        eyeClassifier.setup()
    }

    data class Result(
        var leftCovered: Boolean = false,
        var rightCovered: Boolean = false,
        var debugText: String = ""
    )

    fun analyze(
        leftEyeBitmap: Bitmap?,
        rightEyeBitmap: Bitmap?
    ): Result {

        val result = Result()

        // ✅ 분류
        val left = eyeClassifier.classify(leftEyeBitmap)
        val right = eyeClassifier.classify(rightEyeBitmap)

        result.leftCovered = left.isCovered
        result.rightCovered = right.isCovered

        // ✅ 디버그
        result.debugText = """
LEFT: ${left.debug}
RIGHT: ${right.debug}
""".trimIndent()

        // ✅ 데이터 저장 (🔥 핵심)
        saver.save(leftEyeBitmap, currentLabel)
        saver.save(rightEyeBitmap, currentLabel)

        return result
    }
}