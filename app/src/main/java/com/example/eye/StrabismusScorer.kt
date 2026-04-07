package com.example.eye

object StrabismusScorer {

    fun score(
        bothEyesReady: Boolean,
        alignmentScore: Float,
        reflectionScore: Float,
        coverScore: Float
    ): StrabismusScoreResult {

        if (!bothEyesReady) {
            return StrabismusScoreResult(
                score = 0f,
                suspected = false,
                label = "판정불가",
                reason = "양쪽 눈 준비 상태가 충분하지 않습니다."
            )
        }

        val finalScore = (
                alignmentScore * 0.45f +
                        reflectionScore * 0.30f +
                        coverScore * 0.25f
                ).coerceIn(0f, 1f)

        val suspected = finalScore >= 0.45f

        val label = when {
            finalScore >= 0.75f -> "강한 의심"
            finalScore >= 0.45f -> "의심"
            else -> "정상"
        }

        val reason = when {
            !suspected -> "정렬, 반사광, 가림 검사 결과가 전반적으로 안정적입니다."
            finalScore >= 0.75f -> "여러 검사 항목에서 일관되게 큰 차이가 관찰됩니다."
            else -> "일부 검사 항목에서 좌우 눈 차이가 관찰됩니다."
        }

        return StrabismusScoreResult(
            score = finalScore,
            suspected = suspected,
            label = label,
            reason = reason
        )
    }
}