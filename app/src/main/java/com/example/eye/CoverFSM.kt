package com.example.eye

enum class CoverEventState {
    WAIT_COVER_1,
    WAIT_UNCOVER_1,
    WAIT_COVER_2,
    WAIT_UNCOVER_2,
    DONE
}

class CoverFSM {

    private var state = CoverEventState.WAIT_COVER_1
    private var completed = false

    fun reset() {
        state = CoverEventState.WAIT_COVER_1
        completed = false
    }

    fun update(isCovered: Boolean): Boolean {
        if (completed) return true

        when (state) {
            CoverEventState.WAIT_COVER_1 -> {
                if (isCovered) state = CoverEventState.WAIT_UNCOVER_1
            }
            CoverEventState.WAIT_UNCOVER_1 -> {
                if (!isCovered) state = CoverEventState.WAIT_COVER_2
            }
            CoverEventState.WAIT_COVER_2 -> {
                if (isCovered) state = CoverEventState.WAIT_UNCOVER_2
            }
            CoverEventState.WAIT_UNCOVER_2 -> {
                if (!isCovered) {
                    state = CoverEventState.DONE
                    completed = true
                }
            }
            CoverEventState.DONE -> {
                completed = true
            }
        }

        return completed
    }

    fun getStateName(): String = state.name
}