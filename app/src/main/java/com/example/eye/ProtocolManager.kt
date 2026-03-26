package com.example.eye

class ProtocolManager {

    private var phase: ScreeningPhase = ScreeningPhase.ALIGN
    private var phaseStartTime: Long = System.currentTimeMillis()

    private var finalLabel: String = "-"
    private var finalScore: Int = 0

    fun reset() {
        phase = ScreeningPhase.ALIGN
        phaseStartTime = System.currentTimeMillis()
        finalLabel = "-"
        finalScore = 0
    }

    private fun phaseElapsedSec(): Double {
        return (System.currentTimeMillis() - phaseStartTime) / 1000.0
    }

    private fun transition(nextPhase: ScreeningPhase) {
        phase = nextPhase
        phaseStartTime = System.currentTimeMillis()
    }

    fun update(
        faceDetected: Boolean,
        faceCentered: Boolean,
        leftEyeOpenRatio: Float,
        rightEyeOpenRatio: Float,
        leftIrisVisible: Boolean,
        rightIrisVisible: Boolean
    ): Triple<String, String, String> {

        val elapsed = phaseElapsedSec()

        when (phase) {
            ScreeningPhase.ALIGN -> {
                if (faceDetected && faceCentered && elapsed >= 1.5) {
                    transition(ScreeningPhase.BASELINE)
                }
            }

            ScreeningPhase.BASELINE -> {
                if (elapsed >= 2.5) {
                    transition(ScreeningPhase.CURRENT)
                }
            }

            ScreeningPhase.CURRENT -> {
                if (elapsed >= 2.5) {
                    transition(ScreeningPhase.REFLECTION)
                }
            }

            ScreeningPhase.REFLECTION -> {
                if (elapsed >= 2.0) {
                    transition(ScreeningPhase.COVER_LEFT)
                }
            }

            ScreeningPhase.COVER_LEFT -> {
                val rightCovered = !rightIrisVisible || rightEyeOpenRatio < 0.08f
                if (rightCovered && elapsed >= 2.0) {
                    transition(ScreeningPhase.COVER_RIGHT)
                }
            }

            ScreeningPhase.COVER_RIGHT -> {
                val leftCovered = !leftIrisVisible || leftEyeOpenRatio < 0.08f
                if (leftCovered && elapsed >= 2.0) {
                    finalScore = 1
                    finalLabel = "의심"
                    transition(ScreeningPhase.RESULT)
                }
            }

            ScreeningPhase.RESULT -> Unit
        }

        val guideMessage = when (phase) {
            ScreeningPhase.ALIGN -> {
                when {
                    !faceDetected -> "얼굴을 화면 중앙에 맞춰주세요"
                    !faceCentered -> "얼굴을 중앙에 맞춰주세요"
                    else -> "얼굴을 중앙에 유지해주세요"
                }
            }

            ScreeningPhase.BASELINE -> "정면을 보고 눈을 편하게 유지해주세요"
            ScreeningPhase.CURRENT -> "계속 정면을 유지해주세요"
            ScreeningPhase.REFLECTION -> "정면을 유지한 채 반사광 검사를 진행합니다"
            ScreeningPhase.COVER_LEFT -> "오른쪽 눈을 손으로 가려주세요"
            ScreeningPhase.COVER_RIGHT -> "왼쪽 눈을 손으로 가려주세요"
            ScreeningPhase.RESULT -> "검사 결과: $finalLabel (점수: $finalScore)"
        }

        val debugText = buildString {
            append("phase: ${phase.name}\n")
            append("elapsed: %.1fs\n".format(phaseElapsedSec()))
            append("faceDetected: $faceDetected\n")
            append("faceCentered: $faceCentered\n")
            append("leftOpen: %.3f\n".format(leftEyeOpenRatio))
            append("rightOpen: %.3f\n".format(rightEyeOpenRatio))
            append("leftIris: $leftIrisVisible\n")
            append("rightIris: $rightIrisVisible")
        }

        return Triple(guideMessage, debugText, phase.name)
    }
}