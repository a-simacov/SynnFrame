package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import java.time.LocalDateTime

/**
 * Состояние визарда действий для использования в UI
 */
data class ActionWizardState(
    val taskId: String = "",
    val actionId: String = "",
    val action: PlannedAction? = null,
    val steps: List<WizardStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val results: Map<String, Any> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val isInitialized: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isProcessingStep: Boolean = false,
    val isSending: Boolean = false,
    val sendError: String? = null
) {
    /**
     * Возвращает текущий шаг или null, если визард завершен
     */
    val currentStep: WizardStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    /**
     * Определяет, завершен ли визард
     */
    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    /**
     * Возвращает прогресс выполнения визарда
     */
    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    /**
     * Определяет, можно ли вернуться назад
     */
    val canGoBack: Boolean
        get() = currentStepIndex > 0 && currentStep?.canNavigateBack ?: true
}