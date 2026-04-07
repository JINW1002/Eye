package com.example.eye

class ProtocolManager {

    private var phase = ScreeningPhase.ALIGN_FRONT
    private var phaseStartTime = System.currentTimeMillis()
    private var stableStartTime: Long? = null

    private var finalLabel = "판정불가"
    private var finalScore = 0f
    private var finalReason = "검사 전"

    fun reset() {
        phase = ScreeningPhase.ALIGN_FRONT
        phaseStartTime = System.currentTimeMillis()
        stableStartTime = null
        finalLabel = "판정불가"
        finalScore = 0f
        finalReason = "검사 전"
    }

    fun isInResultPhase(): Boolean = phase == ScreeningPhase.RESULT
    fun getFinalLabel(): String = finalLabel
    fun getFinalScore(): Float = finalScore
    fun getFinalReason(): String = finalReason

    private fun now(): Long = System.currentTimeMillis()
    private fun elapsedMs(): Long = now() - phaseStartTime

    private fun transition(next: ScreeningPhase) {
        phase = next
        phaseStartTime = now()
        stableStartTime = null
    }

    private fun requestedCameraModeForPhase(): CameraMode {
        return when (phase) {
            ScreeningPhase.REFLECTION_BACK_PREPARE,
            ScreeningPhase.REFLECTION_BACK_CAPTURE -> CameraMode.BACK
            else -> CameraMode.FRONT
        }
    }

    private fun restartCurrentPhase(): Triple<String, CameraMode, Boolean> {
        phaseStartTime = now()
        stableStartTime = null

        val requestedCameraMode = requestedCameraModeForPhase()
        val torchOn = phase == ScreeningPhase.REFLECTION_BACK_CAPTURE

        val bottom = when (phase) {
            ScreeningPhase.ALIGN_FRONT ->
                "얼굴과 양쪽 눈이 잘 보이도록 다시 맞춰주세요."

            ScreeningPhase.REFLECTION_BACK_PREPARE ->
                "후면 카메라에서 양쪽 눈이 잘 보이도록 다시 맞춰주세요."

            ScreeningPhase.REFLECTION_BACK_CAPTURE ->
                "반사광 검사를 다시 진행합니다. 눈을 안정적으로 유지해주세요."

            ScreeningPhase.COVER_RIGHT_PREPARE,
            ScreeningPhase.COVER_RIGHT_COVER_1,
            ScreeningPhase.COVER_RIGHT_UNCOVER_1,
            ScreeningPhase.COVER_RIGHT_COVER_2,
            ScreeningPhase.COVER_RIGHT_UNCOVER_2 ->
                "오른쪽 눈 검사를 다시 시작합니다."

            ScreeningPhase.COVER_LEFT_PREPARE,
            ScreeningPhase.COVER_LEFT_COVER_1,
            ScreeningPhase.COVER_LEFT_UNCOVER_1,
            ScreeningPhase.COVER_LEFT_COVER_2,
            ScreeningPhase.COVER_LEFT_UNCOVER_2 ->
                "왼쪽 눈 검사를 다시 시작합니다."

            ScreeningPhase.RESULT ->
                "결과 화면입니다."
        }

        return Triple("현재 단계를 다시 수행합니다.\n$bottom", requestedCameraMode, torchOn)
    }

    private fun isStableFor(condition: Boolean, requiredMs: Long): Boolean {
        val current = now()

        if (!condition) {
            stableStartTime = null
            return false
        }

        if (stableStartTime == null) {
            stableStartTime = current
            return false
        }

        return (current - stableStartTime!!) >= requiredMs
    }

    private fun makeMessage(top: String, bottom: String): String {
        return "$top\n$bottom"
    }

    private fun readyCommon(
        activeCameraMode: CameraMode,
        targetMode: CameraMode,
        faceDetected: Boolean,
        faceCentered: Boolean,
        bothEyeRoiValid: Boolean
    ): Boolean {
        return activeCameraMode == targetMode &&
                faceDetected &&
                faceCentered &&
                bothEyeRoiValid
    }

    private fun eyeQualityGuide(
        faceDetected: Boolean,
        faceCentered: Boolean,
        bothEyeRoiValid: Boolean,
        roiQualityReason: String,
        targetMode: CameraMode
    ): String {
        return when {
            !faceDetected -> {
                if (targetMode == CameraMode.BACK) {
                    "후면 카메라에서 얼굴을 보여주세요."
                } else {
                    "얼굴을 화면 중앙에 맞춰주세요."
                }
            }

            !faceCentered -> {
                if (targetMode == CameraMode.BACK) {
                    "후면 카메라에서 얼굴을 중앙에 맞춰주세요."
                } else {
                    "얼굴을 중앙에 맞춰주세요."
                }
            }

            !bothEyeRoiValid -> {
                "양쪽 눈이 모두 잘 보이게 해주세요.\n$roiQualityReason\n눈을 더 가까이 보여주세요."
            }

            else -> "얼굴과 눈이 확인되었습니다."
        }
    }

    private fun finalizeResult(
        accumulatedScore: Float,
        accumulatedFrameCount: Int,
        accumulatedLabel: String,
        accumulatedReason: String
    ) {
        finalScore = accumulatedScore
        finalLabel = if (accumulatedFrameCount < 5) "판정불가" else accumulatedLabel
        finalReason = if (accumulatedFrameCount < 5) {
            "유효 프레임 수가 부족하여 최종 판정을 내리기 어렵습니다."
        } else {
            accumulatedReason
        }
    }

    fun update(
        activeCameraMode: CameraMode,
        faceDetected: Boolean,
        faceCentered: Boolean,
        bothEyeRoiValid: Boolean,
        leftEyeRoiScore: Float,
        rightEyeRoiScore: Float,
        roiQualityReason: String,
        leftEyeOpenRatio: Float,
        rightEyeOpenRatio: Float,
        leftIrisVisible: Boolean,
        rightIrisVisible: Boolean,
        accumulatedScore: Float,
        accumulatedFrameCount: Int,
        accumulatedLabel: String,
        accumulatedReason: String
    ): Triple<String, CameraMode, Boolean> {

        val requestedCameraMode = requestedCameraModeForPhase()
        val torchOn = phase == ScreeningPhase.REFLECTION_BACK_CAPTURE

        when (phase) {
            ScreeningPhase.ALIGN_FRONT -> {
                val ready = readyCommon(
                    activeCameraMode, CameraMode.FRONT, faceDetected, faceCentered, bothEyeRoiValid
                )

                if (!ready) {
                    return Triple(
                        makeMessage(
                            eyeQualityGuide(
                                faceDetected,
                                faceCentered,
                                bothEyeRoiValid,
                                roiQualityReason,
                                CameraMode.FRONT
                            ),
                            "양쪽 눈 ROI 품질이 안정적으로 확보되면 다음 단계로 넘어갑니다."
                        ),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (isStableFor(ready, 1200L)) {
                    transition(ScreeningPhase.REFLECTION_BACK_PREPARE)
                    return Triple(
                        makeMessage("얼굴이 확인되었습니다.", "후면 카메라 단계로 전환합니다."),
                        requestedCameraModeForPhase(),
                        false
                    )
                }

                if (elapsedMs() >= 7000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("얼굴이 확인되었습니다.", "양쪽 눈이 잘 보입니다. 결과가 나오면 다음 단계로 넘어갑니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.REFLECTION_BACK_PREPARE -> {
                val ready = readyCommon(
                    activeCameraMode, CameraMode.BACK, faceDetected, faceCentered, bothEyeRoiValid
                )

                if (!ready) {
                    return Triple(
                        makeMessage(
                            eyeQualityGuide(
                                faceDetected,
                                faceCentered,
                                bothEyeRoiValid,
                                roiQualityReason,
                                CameraMode.BACK
                            ),
                            "후면 카메라에서 양쪽 눈이 안정적으로 보이면 반사광 검사를 시작합니다."
                        ),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (isStableFor(ready, 1200L)) {
                    transition(ScreeningPhase.REFLECTION_BACK_CAPTURE)
                    return Triple(
                        makeMessage("얼굴이 확인되었습니다.", "반사광 검사를 시작합니다. 결과가 나오면 다음 단계로 넘어갑니다."),
                        requestedCameraModeForPhase(),
                        true
                    )
                }

                if (elapsedMs() >= 8000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("얼굴이 확인되었습니다.", "후면 카메라 준비가 진행 중입니다. 그대로 유지해주세요."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.REFLECTION_BACK_CAPTURE -> {
                val ready = readyCommon(
                    activeCameraMode, CameraMode.BACK, faceDetected, faceCentered, bothEyeRoiValid
                )

                if (!ready) {
                    return Triple(
                        makeMessage(
                            eyeQualityGuide(
                                faceDetected,
                                faceCentered,
                                bothEyeRoiValid,
                                roiQualityReason,
                                CameraMode.BACK
                            ),
                            "반사광 검사를 진행하려면 양쪽 눈이 안정적으로 보여야 합니다."
                        ),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (isStableFor(ready, 1800L)) {
                    transition(ScreeningPhase.COVER_RIGHT_PREPARE)
                    return Triple(
                        makeMessage("얼굴이 확인되었습니다.", "오른쪽 눈 가림 검사를 준비합니다."),
                        requestedCameraModeForPhase(),
                        false
                    )
                }

                if (elapsedMs() >= 9000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("얼굴이 확인되었습니다.", "반사광 검사가 진행 중입니다. 결과가 나오면 다음 단계로 넘어갑니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_RIGHT_PREPARE -> {
                val ready = readyCommon(
                    activeCameraMode, CameraMode.FRONT, faceDetected, faceCentered, bothEyeRoiValid
                )

                if (!ready) {
                    return Triple(
                        makeMessage(
                            eyeQualityGuide(
                                faceDetected,
                                faceCentered,
                                bothEyeRoiValid,
                                roiQualityReason,
                                CameraMode.FRONT
                            ),
                            "오른쪽 눈 검사를 준비하려면 양쪽 눈이 안정적으로 보여야 합니다."
                        ),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (isStableFor(ready, 1000L)) {
                    transition(ScreeningPhase.COVER_RIGHT_COVER_1)
                    return Triple(
                        makeMessage("오른쪽 눈을 가리세요.", "얼굴과 눈이 확인되었습니다. 안내에 따라 검사합니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (elapsedMs() >= 7000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("얼굴이 확인되었습니다.", "오른쪽 눈 검사를 준비 중입니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_RIGHT_COVER_1 -> {
                val rightCovered = (!rightIrisVisible) || (rightEyeOpenRatio < 0.10f)
                if (rightCovered) {
                    transition(ScreeningPhase.COVER_RIGHT_UNCOVER_1)
                    return Triple(
                        makeMessage("오른쪽 눈을 떼세요.", "첫 번째 가림이 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("오른쪽 눈을 가리세요.", "ROI 점수 L: %.2f / R: %.2f".format(leftEyeRoiScore, rightEyeRoiScore)),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_RIGHT_UNCOVER_1 -> {
                val rightCovered = (!rightIrisVisible) || (rightEyeOpenRatio < 0.10f)
                if (!rightCovered) {
                    transition(ScreeningPhase.COVER_RIGHT_COVER_2)
                    return Triple(
                        makeMessage("오른쪽 눈을 다시 가리세요.", "첫 번째 떼기가 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("오른쪽 눈을 떼세요.", "천천히 눈을 다시 보여주세요."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_RIGHT_COVER_2 -> {
                val rightCovered = (!rightIrisVisible) || (rightEyeOpenRatio < 0.10f)
                if (rightCovered) {
                    transition(ScreeningPhase.COVER_RIGHT_UNCOVER_2)
                    return Triple(
                        makeMessage("오른쪽 눈을 다시 떼세요.", "두 번째 가림이 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("오른쪽 눈을 다시 가리세요.", "안내에 따라 검사를 진행합니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_RIGHT_UNCOVER_2 -> {
                val rightCovered = (!rightIrisVisible) || (rightEyeOpenRatio < 0.10f)
                if (!rightCovered) {
                    transition(ScreeningPhase.COVER_LEFT_PREPARE)
                    return Triple(
                        makeMessage("오른쪽 눈 검사가 완료되었습니다.", "왼쪽 눈 가림 검사를 준비합니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("오른쪽 눈을 다시 떼세요.", "눈을 다시 보여주세요."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_LEFT_PREPARE -> {
                val ready = readyCommon(
                    activeCameraMode, CameraMode.FRONT, faceDetected, faceCentered, bothEyeRoiValid
                )

                if (!ready) {
                    return Triple(
                        makeMessage(
                            eyeQualityGuide(
                                faceDetected,
                                faceCentered,
                                bothEyeRoiValid,
                                roiQualityReason,
                                CameraMode.FRONT
                            ),
                            "왼쪽 눈 검사를 준비하려면 양쪽 눈이 안정적으로 보여야 합니다."
                        ),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (isStableFor(ready, 1000L)) {
                    transition(ScreeningPhase.COVER_LEFT_COVER_1)
                    return Triple(
                        makeMessage("왼쪽 눈을 가리세요.", "얼굴과 눈이 확인되었습니다. 안내에 따라 검사합니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }

                if (elapsedMs() >= 7000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("얼굴이 확인되었습니다.", "왼쪽 눈 검사를 준비 중입니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_LEFT_COVER_1 -> {
                val leftCovered = (!leftIrisVisible) || (leftEyeOpenRatio < 0.10f)
                if (leftCovered) {
                    transition(ScreeningPhase.COVER_LEFT_UNCOVER_1)
                    return Triple(
                        makeMessage("왼쪽 눈을 떼세요.", "첫 번째 가림이 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("왼쪽 눈을 가리세요.", "ROI 점수 L: %.2f / R: %.2f".format(leftEyeRoiScore, rightEyeRoiScore)),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_LEFT_UNCOVER_1 -> {
                val leftCovered = (!leftIrisVisible) || (leftEyeOpenRatio < 0.10f)
                if (!leftCovered) {
                    transition(ScreeningPhase.COVER_LEFT_COVER_2)
                    return Triple(
                        makeMessage("왼쪽 눈을 다시 가리세요.", "첫 번째 떼기가 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("왼쪽 눈을 떼세요.", "천천히 눈을 다시 보여주세요."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_LEFT_COVER_2 -> {
                val leftCovered = (!leftIrisVisible) || (leftEyeOpenRatio < 0.10f)
                if (leftCovered) {
                    transition(ScreeningPhase.COVER_LEFT_UNCOVER_2)
                    return Triple(
                        makeMessage("왼쪽 눈을 다시 떼세요.", "두 번째 가림이 확인되었습니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("왼쪽 눈을 다시 가리세요.", "안내에 따라 검사를 진행합니다."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.COVER_LEFT_UNCOVER_2 -> {
                val leftCovered = (!leftIrisVisible) || (leftEyeOpenRatio < 0.10f)
                if (!leftCovered) {
                    finalizeResult(
                        accumulatedScore,
                        accumulatedFrameCount,
                        accumulatedLabel,
                        accumulatedReason
                    )
                    transition(ScreeningPhase.RESULT)
                    return Triple(
                        makeMessage("왼쪽 눈 검사가 완료되었습니다.", "검사 결과를 표시합니다."),
                        requestedCameraMode,
                        torchOn
                    )
                }
                if (elapsedMs() >= 12000L) return restartCurrentPhase()

                return Triple(
                    makeMessage("왼쪽 눈을 다시 떼세요.", "눈을 다시 보여주세요."),
                    requestedCameraMode,
                    torchOn
                )
            }

            ScreeningPhase.RESULT -> {
                val resultText = when (finalLabel) {
                    "강한 의심" -> "검사 결과는 강한 의심입니다."
                    "의심" -> "검사 결과는 의심입니다."
                    "정상" -> "검사 결과는 정상입니다."
                    else -> "검사 결과는 판정불가입니다."
                }

                return Triple(
                    makeMessage(resultText, finalReason),
                    requestedCameraMode,
                    false
                )
            }
        }
    }
}