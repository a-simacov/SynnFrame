package com.synngate.synnframe.domain.model.wizard

import timber.log.Timber

/**
 * Структура для хранения всех коллбэков, необходимых ActionContext
 */
data class WizardCallbacks(
    val onUpdate: (Map<String, Any>) -> Unit,
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit,
    val onForward: () -> Unit,
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit
)

/**
 * Класс для эффективного управления контекстом визарда.
 * Вместо создания нового контекста для каждого шага, хранит базовый контекст
 * и обновляет только изменяющиеся данные.
 */
class ActionContextHolder(
    val taskId: String,
    val actionId: String,
    private val callbacks: WizardCallbacks
) {
    // Базовый контекст, содержащий данные, общие для всех шагов
    private val baseContext = ActionContext(
        taskId = taskId,
        actionId = actionId,
        stepId = "",
        results = emptyMap(),
        hasStepResult = false,
        onUpdate = callbacks.onUpdate,
        onComplete = callbacks.onComplete,
        onBack = callbacks.onBack,
        onForward = callbacks.onForward,
        onSkip = callbacks.onSkip,
        onCancel = callbacks.onCancel,
        isProcessingStep = false,
        isFirstStep = false
    )

    /**
     * Создает контекст для конкретного шага, обновляя только нужные поля
     * вместо создания полной копии всех данных.
     */
    fun getContextForStep(
        stepId: String,
        results: Map<String, Any>,
        hasStepResult: Boolean,
        validationError: String? = null,
        lastScannedBarcode: String? = null,
        isProcessingStep: Boolean = false,
        isFirstStep: Boolean = false
    ): ActionContext {
        Timber.d("Creating context for step: $stepId")

        // Создаем копию только с обновленными полями
        return baseContext.copy(
            stepId = stepId,
            results = results,
            hasStepResult = hasStepResult,
            validationError = validationError,
            lastScannedBarcode = lastScannedBarcode,
            isProcessingStep = isProcessingStep,
            isFirstStep = isFirstStep
        )
    }
}