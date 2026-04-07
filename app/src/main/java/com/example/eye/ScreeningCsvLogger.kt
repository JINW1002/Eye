package com.example.eye

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreeningCsvLogger(private val context: Context) {

    private val fileName = "screening_results.csv"

    fun append(result: AnalysisResult) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)

        val isNewFile = !file.exists()

        FileWriter(file, true).use { writer ->
            if (isNewFile) {
                writer.append(
                    "timestamp,final_label,final_score,final_reason," +
                            "accumulated_score,accumulated_frame_count,accumulated_suspected," +
                            "strabismus_score,strabismus_label,strabismus_suspected," +
                            "alignment_score,alignment_suspected,iris_horizontal_diff,iris_vertical_diff," +
                            "reflection_score,reflection_suspected,reflection_reason," +
                            "cover_score,cover_suspected,right_cover_shift,left_cover_shift,cover_reason," +
                            "left_eye_roi_score,right_eye_roi_score,both_eye_roi_valid,roi_quality_reason\n"
                )
            }

            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            writer.append(
                listOf(
                    escape(timestamp),
                    escape(result.finalResultLabel),
                    result.finalResultScore.toString(),
                    escape(result.finalResultReason),

                    result.accumulatedScore.toString(),
                    result.accumulatedFrameCount.toString(),
                    result.accumulatedSuspected.toString(),

                    result.strabismusScore.toString(),
                    escape(result.strabismusLabel),
                    result.strabismusSuspected.toString(),

                    result.alignmentScore.toString(),
                    result.alignmentSuspected.toString(),
                    result.irisHorizontalDiff.toString(),
                    result.irisVerticalDiff.toString(),

                    result.reflectionScore.toString(),
                    result.reflectionSuspected.toString(),
                    escape(result.reflectionReason),

                    result.coverScore.toString(),
                    result.coverSuspected.toString(),
                    result.rightCoverShift.toString(),
                    result.leftCoverShift.toString(),
                    escape(result.coverReason),

                    result.leftEyeRoiScore.toString(),
                    result.rightEyeRoiScore.toString(),
                    result.bothEyeRoiValid.toString(),
                    escape(result.roiQualityReason)
                ).joinToString(",")
            )
            writer.append("\n")
        }
    }

    private fun escape(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }
}