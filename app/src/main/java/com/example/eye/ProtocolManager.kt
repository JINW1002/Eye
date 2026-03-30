package com.example.eye

class ProtocolManager {

    private var phase = ScreeningPhase.ALIGN_FRONT
    private var phaseStartTime = System.currentTimeMillis()

    private val coverRightFsm = CoverFSM()
    private val coverLeftFsm = CoverFSM()

    private var finalLabel = "-"
    private var finalScore = 0

    fun reset() {
        phase = ScreeningPhase.ALIGN_FRONT
        phaseStartTime = System.currentTimeMillis()
        coverRightFsm.reset()
        coverLeftFsm.reset()
        finalLabel = "-"
        finalScore = 0
    }

    private fun elapsedSec(): Double {
        return (System.currentTimeMillis() - phaseStartTime) / 1000.0
    }

    private fun transition(next: ScreeningPhase) {
        phase = next
        phaseStartTime = System.currentTimeMillis()
    }

    fun update(
        faceDetected: Boolean,
        faceCentered: Boolean,
        leftEyeOpenRatio: Float,
        rightEyeOpenRatio: Float,
        leftIrisVisible: Boolean,
        rightIrisVisible: Boolean
    ): Triple<String, CameraMode, Boolean> {

        val elapsed = elapsedSec()

        when (phase) {
            ScreeningPhase.ALIGN_FRONT -> {
                if (faceDetected && faceCentered && elapsed >= 1.5) {
                    transition(ScreeningPhase.REFLECTION_BACK_PREPARE)
                }
            }

            ScreeningPhase.REFLECTION_BACK_PREPARE -> {
                if (elapsed >= 1.5) {
                    transition(ScreeningPhase.REFLECTION_BACK_CAPTURE)
                }
            }

            ScreeningPhase.REFLECTION_BACK_CAPTURE -> {
                if (elapsed >= 2.5) {
                    transition(ScreeningPhase.COVER_RIGHT_PREPARE)
                }
            }

            ScreeningPhase.COVER_RIGHT_PREPARE -> {
                coverRightFsm.reset()
                if (elapsed >= 1.0) {
                    transition(ScreeningPhase.COVER_RIGHT_TEST)
                }
            }

            ScreeningPhase.COVER_RIGHT_TEST -> {
                val rightCovered = (!rightIrisVisible) || rightEyeOpenRatio < 0.10f
                val done = coverRightFsm.update(rightCovered)
                if (done) {
                    transition(ScreeningPhase.COVER_LEFT_PREPARE)
                }
            }

            ScreeningPhase.COVER_LEFT_PREPARE -> {
                coverLeftFsm.reset()
                if (elapsed >= 1.0) {
                    transition(ScreeningPhase.COVER_LEFT_TEST)
                }
            }

            ScreeningPhase.COVER_LEFT_TEST -> {
                val leftCovered = (!leftIrisVisible) || leftEyeOpenRatio < 0.10f
                val done = coverLeftFsm.update(leftCovered)
                if (done) {
                    finalScore = 1
                    finalLabel = "의심"
                    transition(ScreeningPhase.RESULT)
                }
            }

            ScreeningPhase.RESULT -> Unit
        }

        val guideMessage = when (phase) {
            ScreeningPhase.ALIGN_FRONT -> {
                when {
                    !faceDetected -> "얼굴을 화면 중앙에 맞춰주세요"
                    !faceCentered -> "얼굴을 중앙에 맞춰주세요"
                    else -> "얼굴을 중앙에 유지해주세요"
                }
            }

            ScreeningPhase.REFLECTION_BACK_PREPARE ->
                "후면 카메라로 전환합니다. 카메라를 눈 쪽으로 향해주세요"

            ScreeningPhase.REFLECTION_BACK_CAPTURE ->
                "후면 카메라와 플래시로 반사광 검사를 진행합니다. 그대로 유지해주세요"

            ScreeningPhase.COVER_RIGHT_PREPARE ->
                "전면 카메라로 전환합니다. 오른쪽 눈을 빠르게 두 번 가릴 준비를 해주세요"

            ScreeningPhase.COVER_RIGHT_TEST ->
                "오른쪽 눈을 가리고 떼고, 다시 가리고 떼주세요"

            ScreeningPhase.COVER_LEFT_PREPARE ->
                "왼쪽 눈을 빠르게 두 번 가릴 준비를 해주세요"

            ScreeningPhase.COVER_LEFT_TEST ->
                "왼쪽 눈을 가리고 떼고, 다시 가리고 떼주세요"

            ScreeningPhase.RESULT ->
                "검사 결과: $finalLabel (점수: $finalScore)"
        }

        val cameraMode = when (phase) {
            ScreeningPhase.REFLECTION_BACK_PREPARE,
            ScreeningPhase.REFLECTION_BACK_CAPTURE -> CameraMode.BACK
            else -> CameraMode.FRONT
        }

        val torchOn = phase == ScreeningPhase.REFLECTION_BACK_CAPTURE

        val debugText = buildString {
            append("phase: ${phase.name}\n")
            append("elapsed: %.1fs\n".format(elapsedSec()))
            append("faceDetected: $faceDetected\n")
            append("faceCentered: $faceCentered\n")
            append("leftOpen: %.3f\n".format(leftEyeOpenRatio))
            append("rightOpen: %.3f\n".format(rightEyeOpenRatio))
            append("leftIris: $leftIrisVisible\n")
            append("rightIris: $rightIrisVisible\n")
            append("coverRightFSM: ${coverRightFsm.getStateName()}\n")
            append("coverLeftFSM: ${coverLeftFsm.getStateName()}\n")
            append("cameraMode: ${cameraMode.name}\n")
            append("torchOn: $torchOn")
        }

        return Triple(guideMessage + "\n\n" + debugText, cameraMode, torchOn)
    }
}