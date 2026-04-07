package com.example.eye

class StrabismusScoreAccumulator(
    private val maxFrames: Int = 20
) {
    private val scores = ArrayDeque<Float>()

    fun reset() {
        scores.clear()
    }

    fun addScore(
        validFrame: Boolean,
        score: Float
    ): AccumulatedScoreResult {
        if (validFrame) {
            scores.addLast(score)
            while (scores.size > maxFrames) {
                scores.removeFirst()
            }
        }

        return currentResult()
    }

    fun currentResult(): AccumulatedScoreResult {
        if (scores.isEmpty()) {
            return AccumulatedScoreResult(
                averageScore = 0f,
                frameCount = 0,
                suspected = false,
                label = "판정불가",
                reason = "누적된 유효 프레임이 없습니다."
            )
        }

        val avg = scores.average().toFloat()

        val label = when {
            avg >= 0.75f -> "강한 의심"
            avg >= 0.45f -> "의심"
            else -> "정상"
        }

        val suspected = avg >= 0.45f

        val reason = when {
            scores.size < 5 -> "유효 프레임을 더 수집하는 중입니다."
            suspected -> "여러 프레임 평균에서 사시 의심 점수가 높습니다."
            else -> "여러 프레임 평균에서 좌우 눈 정렬이 비교적 안정적입니다."
        }

        return AccumulatedScoreResult(
            averageScore = avg,
            frameCount = scores.size,
            suspected = suspected,
            label = label,
            reason = reason
        )
    }
}