package com.example.eye

object ReflectionScorer {

    fun score(
        bothEyesReady: Boolean,
        hirschbergResult: HirschbergResult
    ): ReflectionScoreResult {
        if (!bothEyesReady) {
            return ReflectionScoreResult(
                score = 0f,
                suspected = false,
                reason = "양쪽 눈 준비 상태가 충분하지 않아 반사광 점수를 계산하기 어렵습니다."
            )
        }

        if (!hirschbergResult.valid) {
            return ReflectionScoreResult(
                score = 0f,
                suspected = false,
                reason = hirschbergResult.reason
            )
        }

        return ReflectionScoreResult(
            score = hirschbergResult.score,
            suspected = hirschbergResult.suspected,
            reason = hirschbergResult.reason
        )
    }
}