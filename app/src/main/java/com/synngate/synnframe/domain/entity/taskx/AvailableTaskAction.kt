package com.synngate.synnframe.domain.entity.taskx

enum class AvailableTaskAction {
    PAUSE,              // Приостановить выполнение
    RESUME,             // Продолжить выполнение
    SHOW_PLAN_LINES,    // Показать строки плана
    SHOW_FACT_LINES,    // Показать строки факта
    COMPARE_LINES,      // Сравнить строки плана и факта
    PRINT_TASK_LABEL,   // Печать этикетки задания
    VERIFY_TASK;        // Верифицировать задание

    companion object {
        fun fromString(value: String): AvailableTaskAction {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                SHOW_PLAN_LINES // значение по умолчанию
            }
        }
    }
}