package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import java.time.LocalDateTime

/**
 * Состояние визарда для выполнения действия
 */
data class ActionWizardState(
    val taskId: String = "",
    val actionId: String = "",                // Идентификатор действия
    val action: PlannedAction? = null,        // Запланированное действие
    val steps: List<WizardStep> = emptyList(),// Шаги действия
    val currentStepIndex: Int = 0,
    val results: Map<String, Any> = emptyMap(),// Результаты шагов
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val errors: Map<String, String> = emptyMap(),
    val isInitialized: Boolean = false,
    val lastScannedBarcode: String? = null,    // Последний отсканированный штрихкод
    val isProcessingStep: Boolean = false,
    val isSending: Boolean = false,      // Флаг отправки данных на сервер
    val sendError: String? = null
) {
    val currentStep: WizardStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    val canGoBack: Boolean
        get() = currentStepIndex > 0 && currentStep?.canNavigateBack ?: true

    /**
     * Получает результат для текущего шага
     */
    fun getCurrentStepResult(): Any? {
        return currentStep?.let { results[it.id] }
    }

    /**
     * Проверяет, есть ли результат для указанного шага
     */
    fun hasResultForStep(stepId: String): Boolean {
        return results.containsKey(stepId)
    }

    fun hasErrorForStep(stepId: String): Boolean {
        return errors.containsKey(stepId)
    }

    fun getErrorForStep(stepId: String): String? {
        return errors[stepId]
    }
}