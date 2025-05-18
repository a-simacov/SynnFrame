package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import timber.log.Timber

class WizardContextFactory {
    private val TAG = "WizardContextFactory"

    // Храним контекст-холдер в качестве поля класса для переиспользования между вызовами
    private var contextHolder: ActionContextHolder? = null

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
        val hasResult = state.results.containsKey(stepId)
        val validationError = state.errors[stepId]

        // Проверяем, нужно ли создать новый холдер (изменились ключевые параметры)
        if (contextHolder == null ||
            contextHolder?.taskId != state.taskId ||
            contextHolder?.actionId != state.actionId) {

            Timber.d("$TAG: Creating new ActionContextHolder for task=${state.taskId}, action=${state.actionId}")

            val callbacks = WizardCallbacks(
                onUpdate = { /* Не используется в текущей реализации */ },
                onComplete = { result -> if (result != null) onStepComplete(result) }, // Адаптер для совместимости типов
                onBack = onBack,
                onForward = onForward,
                onSkip = onSkip,
                onCancel = onCancel
            )

            contextHolder = ActionContextHolder(state.taskId, state.actionId, callbacks)
        }

        val enrichedResults = enrichResultsContext(state)

        // Получаем контекст для текущего шага через холдер
        return contextHolder!!.getContextForStep(
            stepId = stepId,
            results = enrichedResults,
            hasStepResult = hasResult,
            validationError = validationError,
            lastScannedBarcode = lastScannedBarcode,
            isProcessingStep = state.isProcessingStep,
            isFirstStep = state.currentStepIndex == 0
        )
    }

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
            onComplete = { _ -> onBack() }, // Игнорируем параметр и вызываем onBack
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
        state: ActionWizardState
    ): Map<String, Any> {
        val results = state.results.toMutableMap()

        // Если отсутствуют специальные ключи, ищем нужные данные в предыдущих шагах
        if (!results.containsKey("lastTaskProduct") || !results.containsKey("lastProduct")) {
            // Пытаемся найти данные в предыдущих шагах
            findObjectsInPreviousSteps(state)?.let { (taskProduct, product) ->
                if (taskProduct != null && !results.containsKey("lastTaskProduct")) {
                    results["lastTaskProduct"] = taskProduct
                }

                if (product != null && !results.containsKey("lastProduct")) {
                    results["lastProduct"] = product
                }
            }
        }

        // Проверяем наличие необходимых данных после обогащения
        if (state.currentStepIndex > 0 &&
            !results.containsKey("lastProduct") &&
            !results.containsKey("lastTaskProduct")) {

            // Экстренное восстановление: ищем данные в запланированном действии
            state.action?.storageProduct?.let { actionProduct ->
                results["emergency_lastTaskProduct"] = actionProduct
                results["emergency_lastProduct"] = actionProduct.product

                // Обычные ключи тоже заполняем
                results["lastTaskProduct"] = actionProduct
                results["lastProduct"] = actionProduct.product
            }
        }

        return results
    }

    private fun findObjectsInPreviousSteps(state: ActionWizardState): Pair<TaskProduct?, Product?>? {
        // Получаем ID предыдущих шагов в обратном порядке (от последнего к первому)
        val previousSteps = state.steps
            .take(state.currentStepIndex)
            .map { it.id }
            .reversed()

        if (previousSteps.isEmpty()) return null

        var foundTaskProduct: TaskProduct? = null
        var foundProduct: Product? = null

        for (stepId in previousSteps) {
            val stepResult = state.results[stepId]

            when (stepResult) {
                is TaskProduct -> {
                    foundTaskProduct = stepResult
                    foundProduct = stepResult.product
                    break
                }
                is Product -> {
                    foundProduct = stepResult
                }
            }
        }

        if (foundProduct != null && foundTaskProduct == null) {
            val existingTaskProduct = state.results.values
                .filterIsInstance<TaskProduct>()
                .firstOrNull { it.product.id == foundProduct.id }

            foundTaskProduct = existingTaskProduct ?: WizardUtils.createTaskProductFromProduct(foundProduct)
        }

        return if (foundTaskProduct != null || foundProduct != null) {
            Pair(foundTaskProduct, foundProduct)
        } else {
            null
        }
    }
}