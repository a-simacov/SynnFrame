// Заменяет com.synngate.synnframe.domain.service.ActionWizardContextFactory
package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import timber.log.Timber

/**
 * Улучшенная фабрика для создания контекста шага визарда.
 * Обеспечивает надежную передачу данных между шагами.
 */
class WizardContextFactory {

    /**
     * Создает контекст для текущего шага на основе состояния визарда
     */
    fun createContext(
        state: ActionWizardState,
        onStepComplete: (Any) -> Unit,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onSkip: (Any?) -> Unit,
        onCancel: () -> Unit,
        lastScannedBarcode: String? = state.lastScannedBarcode
    ): ActionContext {
        val currentStep = state.currentStep ?: return createEmptyContext(state, onBack, onCancel)
        val stepId = currentStep.id
        val hasResult = stepId in state.results
        val validationError = state.errors[stepId]

        // Создаем обогащенный контекст результатов
        val enrichedResults = enrichResultsContext(state, stepId)

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = stepId,
            results = enrichedResults,
            hasStepResult = hasResult,
            onUpdate = { newResults -> updateResultsInState(state, newResults, currentStep.id) },
            onComplete = { result -> handleStepCompletion(result, onStepComplete, onBack) },
            onBack = { onBack() },
            onForward = { onForward() },
            onSkip = { result -> onSkip(result) },
            onCancel = { onCancel() },
            lastScannedBarcode = lastScannedBarcode,
            validationError = validationError,
            isProcessingStep = state.isProcessingStep,
            isFirstStep = state.currentStepIndex == 0
        )
    }

    /**
     * Создает пустой контекст при отсутствии текущего шага
     */
    private fun createEmptyContext(
        state: ActionWizardState,
        onBack: () -> Unit,
        onCancel: () -> Unit
    ): ActionContext {
        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = "",
            results = state.results,
            hasStepResult = false,
            onUpdate = { },
            onComplete = { onBack() },
            onBack = onBack,
            onForward = { },
            onSkip = { },
            onCancel = onCancel,
            isProcessingStep = state.isProcessingStep,
            isFirstStep = false
        )
    }

    /**
     * Обогащает контекст результатов специальными ключами для облегчения доступа к данным
     */
    private fun enrichResultsContext(
        state: ActionWizardState,
        currentStepId: String
    ): Map<String, Any> {
        val results = state.results.toMutableMap()

        // Ищем данные о продукте и TaskProduct для обеспечения
        // надежного доступа в шагах ввода количества
        if (!results.containsKey("lastTaskProduct") || !results.containsKey("lastProduct")) {
            // Ищем в предыдущих шагах
            val previousSteps = state.steps
                .take(state.currentStepIndex)
                .map { it.id }

            for (stepId in previousSteps.reversed()) {
                if (stepId in state.results) {
                    val stepResult = state.results[stepId]
                    if (stepResult is TaskProduct) {
                        results["lastTaskProduct"] = stepResult
                        results["lastProduct"] = stepResult.product
                        break
                    } else if (stepResult is Product) {
                        results["lastProduct"] = stepResult

                        // Пытаемся найти TaskProduct с этим продуктом
                        val taskProduct = state.results.values
                            .filterIsInstance<TaskProduct>()
                            .firstOrNull { it.product.id == stepResult.id }

                        if (taskProduct != null) {
                            results["lastTaskProduct"] = taskProduct
                        }
                        break
                    }
                }
            }
        }

        return results
    }

    /**
     * Обрабатывает завершение шага
     */
    private fun handleStepCompletion(
        result: Any?,
        onStepComplete: (Any) -> Unit,
        onBack: () -> Unit
    ) {
        Timber.d("Step completed with result: ${result?.javaClass?.simpleName}")
        if (result != null) {
            onStepComplete(result)
        } else {
            onBack()
        }
    }

    /**
     * Обновляет результаты в состоянии
     * Примечание: в текущей реализации это пустая функция, так как
     * обновление происходит через onStepComplete
     */
    private fun updateResultsInState(
        state: ActionWizardState,
        newResults: Map<String, Any>,
        stepId: String
    ) {
        // В текущей реализации не используется,
        // так как обновление происходит через машину состояний
    }
}