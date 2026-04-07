package com.example.eye

object StrabismusScorer {

    fun score(
        bothEyesReady: Boolean,
        alignmentScore: Float,
        irisHorizontalDiff: Float,
        irisVerticalDiff: Float
    ): StrabismusScoreResult {

        if (!bothEyesReady) {
            return StrabismusScoreResult(
                score = 0f,
                suspected = false,
                label = "판정불가",
                reason = "양쪽 눈 준비 상태가 충분하지 않습니다."
            )
        }

        val horizontalComponent = (irisHorizontalDiff / 0.18f).coerceIn(0f, 1f)
        val verticalComponent = (irisVerticalDiff / 0.14f).coerceIn(0f, 1f)

        val finalScore = (
                horizontalComponent * 0.7f +
                        verticalComponent * 0.2f +
                        alignmentScore * 0.1f
                ).coerceIn(0f, 1f)

        val suspected = finalScore >= 0.45f

        val label = when {
            finalScore >= 0.75f -> "강한 의심"
            finalScore >= 0.45f -> "의심"
            else -> "정상"
        }

        val reason = when {
            !suspected -> "좌우 눈 정렬 차이가 기준 이내입니다."
            irisHorizontalDiff >= irisVerticalDiff ->
                "수평 방향 홍채 위치 차이가 커서 사시가 의심됩니다."
            else ->
                "수직 방향 홍채 위치 차이가 커서 사시가 의심됩니다."
        }

        return StrabismusScoreResult(
            score = finalScore,
            suspected = suspected,
            label = label,
            reason = reason
        )
    }
}