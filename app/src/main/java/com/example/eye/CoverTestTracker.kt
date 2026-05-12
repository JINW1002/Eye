package com.example.eye

import kotlin.math.sqrt

class CoverTestTracker {

    private var baselineLeftGazeX: Float? = null
    private var baselineLeftGazeY: Float? = null
    private var baselineRightGazeX: Float? = null
    private var baselineRightGazeY: Float? = null

    private var rightCoverShift = 0f
    private var leftCoverShift = 0f

    private var rightCoverRecorded = false
    private var leftCoverRecorded = false

    private val baselineAlpha = 0.15f

    fun reset() {
        baselineLeftGazeX = null
        baselineLeftGazeY = null
        baselineRightGazeX = null
        baselineRightGazeY = null
        rightCoverShift = 0f
        leftCoverShift = 0f
        rightCoverRecorded = false
        leftCoverRecorded = false
    }

    fun updateBaseline(
        bothEyesReady: Boolean,
        noEyeCovered: Boolean,
        leftGazeX: Float,
        leftGazeY: Float,
        rightGazeX: Float,
        rightGazeY: Float
    ) {
        if (!bothEyesReady || !noEyeCovered) return

        baselineLeftGazeX = smooth(baselineLeftGazeX, leftGazeX)
        baselineLeftGazeY = smooth(baselineLeftGazeY, leftGazeY)
        baselineRightGazeX = smooth(baselineRightGazeX, rightGazeX)
        baselineRightGazeY = smooth(baselineRightGazeY, rightGazeY)
    }

    fun recordRightCoverMovement(
        leftEyeReady: Boolean,
        currentLeftGazeX: Float,
        currentLeftGazeY: Float
    ) {
        if (!leftEyeReady) return

        val baseX = baselineLeftGazeX ?: return
        val baseY = baselineLeftGazeY ?: return

        val movement = distance(
            currentLeftGazeX,
            currentLeftGazeY,
            baseX,
            baseY
        )

        if (movement > rightCoverShift) {
            rightCoverShift = movement
            rightCoverRecorded = true
        }
    }

    fun recordLeftCoverMovement(
        rightEyeReady: Boolean,
        currentRightGazeX: Float,
        currentRightGazeY: Float
    ) {
        if (!rightEyeReady) return

        val baseX = baselineRightGazeX ?: return
        val baseY = baselineRightGazeY ?: return

        val movement = distance(
            currentRightGazeX,
            currentRightGazeY,
            baseX,
            baseY
        )

        if (movement > leftCoverShift) {
            leftCoverShift = movement
            leftCoverRecorded = true
        }
    }

    fun currentState(): CoverTestState {
        val maxMovement = maxOf(rightCoverShift, leftCoverShift)
        val averageMovement = when {
            rightCoverRecorded && leftCoverRecorded -> (rightCoverShift + leftCoverShift) / 2f
            rightCoverRecorded -> rightCoverShift
            leftCoverRecorded -> leftCoverShift
            else -> 0f
        }

        val score = (averageMovement / 0.12f).coerceIn(0f, 1f)
        val suspected = score >= 0.45f

        val reason = when {
            baselineLeftGazeX == null || baselineRightGazeX == null ->
                "기준 시선이 아직 충분히 수집되지 않았습니다."
            !rightCoverRecorded && !leftCoverRecorded ->
                "가림 후 반대쪽 눈 움직임이 아직 기록되지 않았습니다."
            suspected ->
                "가림 직후 반대쪽 눈의 재정렬 움직임이 관찰됩니다."
            maxMovement > 0.06f ->
                "작은 움직임은 있으나 기준치 이내입니다."
            else ->
                "가림 직후 반대쪽 눈 움직임이 크지 않습니다."
        }

        return CoverTestState(
            baselineReady = baselineLeftGazeX != null && baselineRightGazeX != null,
            rightCoverShift = rightCoverShift,
            leftCoverShift = leftCoverShift,
            rightCoverRecorded = rightCoverRecorded,
            leftCoverRecorded = leftCoverRecorded,
            coverScore = score,
            suspected = suspected,
            reason = reason
        )
    }

    private fun smooth(previous: Float?, current: Float): Float {
        return if (previous == null) {
            current
        } else {
            previous * (1f - baselineAlpha) + current * baselineAlpha
        }
    }

    private fun distance(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}