package com.synngate.synnframe.domain.entity.taskx

enum class AvailableTaskAction {
    PAUSE,
    RESUME,
    SHOW_PLAN_LINES,
    SHOW_FACT_LINES,
    COMPARE_LINES,
    PRINT_TASK_LABEL,
    VERIFY_TASK;

    companion object {
        fun fromString(value: String): AvailableTaskAction {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                SHOW_PLAN_LINES
            }
        }
    }
}