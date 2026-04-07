package com.example.eye

object ReflectionScorer {

    fun score(
        bothEyesReady: Boolean,
        irisHorizontalDiff: Float,
        irisVerticalDiff: Float
    ): ReflectionScoreResult {
        if (!bothEyesReady) {
            return ReflectionScoreResult(
                score = 0f,
                suspected = false,
                reason = "양쪽 눈 준비 상태가 충분하지 않아 반사광 점수를 계산하기 어렵습니다."
            )
        }

        val horizontal = (irisHorizontalDiff / 0.16f).coerceIn(0f, 1f)
        val vertical = (irisVerticalDiff / 0.12f).coerceIn(0f, 1f)

        val finalScore = (
                horizontal * 0.75f +
                        vertical * 0.25f
                ).coerceIn(0f, 1f)

        val suspected = finalScore >= 0.45f

        val reason = when {
            !suspected -> "반사광 기준 좌우 눈 차이가 크지 않습니다."
            irisHorizontalDiff >= irisVerticalDiff -> "반사광 기준 수평 차이가 커 보입니다."
            else -> "반사광 기준 수직 차이가 커 보입니다."
        }

        return ReflectionScoreResult(
            score = finalScore,
            suspected = suspected,
            reason = reason
        )
    }
}