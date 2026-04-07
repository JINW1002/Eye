package com.example.eye

import kotlin.math.abs

class CoverTestTracker {

    private var baselineHorizontalDiff: Float? = null
    private var baselineVerticalDiff: Float? = null

    private var rightCoverShift: Float = 0f
    private var leftCoverShift: Float = 0f

    private var rightCoverRecorded = false
    private var leftCoverRecorded = false

    fun reset() {
        baselineHorizontalDiff = null
        baselineVerticalDiff = null
        rightCoverShift = 0f
        leftCoverShift = 0f
        rightCoverRecorded = false
        leftCoverRecorded = false
    }

    fun updateBaseline(
        bothEyesReady: Boolean,
        irisHorizontalDiff: Float,
        irisVerticalDiff: Float
    ) {
        if (!bothEyesReady) return
        if (baselineHorizontalDiff == null) {
            baselineHorizontalDiff = irisHorizontalDiff
            baselineVerticalDiff = irisVerticalDiff
        }
    }

    fun recordRightCover(
        bothEyesReady: Boolean,
        currentHorizontalDiff: Float,
        currentVerticalDiff: Float
    ) {
        val baseH = baselineHorizontalDiff ?: return
        val baseV = baselineVerticalDiff ?: return
        if (!bothEyesReady) return

        val shift = abs(currentHorizontalDiff - baseH) + abs(currentVerticalDiff - baseV)
        if (shift > rightCoverShift) {
            rightCoverShift = shift
            rightCoverRecorded = true
        }
    }

    fun recordLeftCover(
        bothEyesReady: Boolean,
        currentHorizontalDiff: Float,
        currentVerticalDiff: Float
    ) {
        val baseH = baselineHorizontalDiff ?: return
        val baseV = baselineVerticalDiff ?: return
        if (!bothEyesReady) return

        val shift = abs(currentHorizontalDiff - baseH) + abs(currentVerticalDiff - baseV)
        if (shift > leftCoverShift) {
            leftCoverShift = shift
            leftCoverRecorded = true
        }
    }

    fun currentState(): CoverTestState {
        val combinedShift = ((rightCoverShift + leftCoverShift) / 2f).coerceIn(0f, 1f)
        val normalizedScore = (combinedShift / 0.18f).coerceIn(0f, 1f)
        val suspected = normalizedScore >= 0.40f

        val reason = when {
            !rightCoverRecorded && !leftCoverRecorded ->
                "가림 검사 변화량이 아직 충분히 수집되지 않았습니다."
            suspected ->
                "가림 전후 눈 정렬 변화량이 커 보입니다."
            else ->
                "가림 전후 눈 정렬 변화량이 크지 않습니다."
        }

        return CoverTestState(
            baselineHorizontalDiff = baselineHorizontalDiff,
            baselineVerticalDiff = baselineVerticalDiff,
            rightCoverShift = rightCoverShift,
            leftCoverShift = leftCoverShift,
            rightCoverRecorded = rightCoverRecorded,
            leftCoverRecorded = leftCoverRecorded,
            coverScore = normalizedScore,
            suspected = suspected,
            reason = reason
        )
    }
}