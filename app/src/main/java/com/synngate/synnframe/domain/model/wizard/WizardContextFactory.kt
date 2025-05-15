package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils

class WizardContextFactory {
    private val TAG = "WizardContextFactory"

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

        val enrichedResults = enrichResultsContext(state)

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = stepId,
            results = enrichedResults,
            hasStepResult = hasResult,
            onUpdate = { /* Не используется в текущей реализации */ },
            onComplete = { result ->
                if (result != null) {
                    onStepComplete(result)
                } else {
                    onBack()
                }
            },
            onBack = { onBack() },
            onForward = {
                val currentData = findBestResultForCurrentStep(state, stepId, enrichedResults)

                if (currentData != null) {
                    onStepComplete(currentData)
                }
            },
            onSkip = { result -> onSkip(result) },
            onCancel = { onCancel() },
            lastScannedBarcode = lastScannedBarcode,
            validationError = validationError,
            isProcessingStep = state.isProcessingStep,
            isFirstStep = state.currentStepIndex == 0
        )
    }

    private fun findBestResultForCurrentStep(
        state: ActionWizardState,
        stepId: String,
        enrichedResults: Map<String, Any>
    ): Any? {
        // 1. Сначала ищем по ключу степа напрямую
        enrichedResults[stepId]?.let { return it }

        // 2. Ищем по типу объекта в зависимости от типа шага
        val step = state.steps.find { it.id == stepId }
        val action = state.action

        if (step != null && action != null) {
            // Находим ActionStep для данного шага
            val actionStep = findActionStep(action, stepId)

            if (actionStep != null) {
                // В зависимости от типа объекта в шаге, ищем подходящий объект
                when (actionStep.objectType) {
                    ActionObjectType.TASK_PRODUCT, ActionObjectType.CLASSIFIER_PRODUCT -> {
                        // Для шагов выбора продукта используем последний выбранный TaskProduct/Product
                        return WizardUtils.findTaskProduct(enrichedResults)
                            ?: WizardUtils.findProduct(enrichedResults)
                    }
                    ActionObjectType.PALLET -> {
                        // Для шагов выбора паллеты используем последнюю выбранную паллету
                        return WizardUtils.findPallet(enrichedResults)
                    }
                    ActionObjectType.BIN -> {
                        // Для шагов выбора ячейки используем последнюю выбранную ячейку
                        return WizardUtils.findBin(enrichedResults)
                    }
                    ActionObjectType.PRODUCT_QUANTITY -> {
                        // Для шагов ввода количества используем TaskProduct с последним введенным количеством
                        val taskProduct = WizardUtils.findTaskProduct(enrichedResults)
                        if (taskProduct != null) {
                            return taskProduct
                        }

                        // Если не нашли TaskProduct, попробуем создать из Product с минимальным количеством
                        val product = WizardUtils.findProduct(enrichedResults)
                        if (product != null) {
                            return WizardUtils.createTaskProductFromProduct(product, 1f)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun findActionStep(action: PlannedAction, stepId: String): ActionStep? {
        action.actionTemplate.storageSteps.find { it.id == stepId }?.let {
            return it
        }

        action.actionTemplate.placementSteps.find { it.id == stepId }?.let {
            return it
        }

        return null
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