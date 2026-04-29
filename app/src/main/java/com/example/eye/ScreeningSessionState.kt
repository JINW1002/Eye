package com.example.eye

class ScreeningSessionState {

    val scoreAccumulator = StrabismusScoreAccumulator(maxFrames = 20)
    val coverTestTracker = CoverTestTracker()

    var savedReflectionScore: Float = 0f
        private set

    var savedReflectionSuspected: Boolean = false
        private set

    var savedReflectionReason: String = "반사광 검사 전"
        private set

    fun updateReflection(result: ReflectionScoreResult) {
        savedReflectionScore = result.score
        savedReflectionSuspected = result.suspected
        savedReflectionReason = result.reason
    }

    fun reset() {
        scoreAccumulator.reset()
        coverTestTracker.reset()

        savedReflectionScore = 0f
        savedReflectionSuspected = false
        savedReflectionReason = "반사광 검사 전"
    }
}