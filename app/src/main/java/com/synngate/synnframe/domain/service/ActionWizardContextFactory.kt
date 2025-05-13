package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import timber.log.Timber

class ActionWizardContextFactory {

// app/src/main/java/com/synngate/synnframe/domain/service/ActionWizardContextFactory.kt

    fun createContext(
        state: ActionWizardState,
        onStepComplete: (Any) -> Unit,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onSkip: (Any?) -> Unit,
        onCancel: () -> Unit,
        lastScannedBarcode: String? = state.lastScannedBarcode
    ): ActionContext {
        val currentStep = state.currentStep
        val stepId = currentStep?.id ?: ""
        val hasResult = state.results.containsKey(stepId)

        val validationError = if (stepId.isNotEmpty()) state.errors[stepId] else null

        // Очень подробное логирование контекста для отладки
        Timber.d("Creating ActionContext with results (полный): ${state.results}")
        Timber.d("Creating ActionContext with results (краткий): ${state.results.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")
        Timber.d("Current step: ${currentStep?.id}, hasResult: $hasResult")

        // Ищем данные, которые должны были быть переданы
        for (key in listOf("lastTaskProduct", "lastProduct", stepId)) {
            if (key in state.results) {
                val value = state.results[key]
                Timber.d("Found key $key in results: ${value?.javaClass?.simpleName}")
            }
        }

        // Проверяем есть ли данные от предыдущих шагов
        if (currentStep != null) {
            val previousSteps = state.steps.subList(0, state.currentStepIndex).map { it.id }
            if (previousSteps.isNotEmpty()) {
                Timber.d("Previous steps: $previousSteps")

                // Ищем данные от этих шагов
                val hasProductData = state.results.any { (key, value) ->
                    key in previousSteps && (value is Product || value is TaskProduct)
                }

                if (!hasProductData) {
                    Timber.w("Warning: No product data found from previous steps!")

                    // Ищем что-либо похожее на продукт в контексте
                    state.results.forEach { (key, value) ->
                        if (value is Product || value is TaskProduct) {
                            Timber.d("Found product-like data with key: $key")
                        }
                    }
                }
            }
        }

        // ВАЖНО: Мы должны убедиться, что все ключи сохраняются
        val contextResults = state.results.toMutableMap()
        // Дополнительно проверяем наличие спец. ключей
        if (stepId.isNotEmpty() && currentStep != null) {
            val prevStepIndex = state.currentStepIndex - 1
            if (prevStepIndex >= 0 && prevStepIndex < state.steps.size) {
                val prevStepId = state.steps[prevStepIndex].id
                if (prevStepId in state.results) {
                    val prevStepValue = state.results[prevStepId]
                    if (prevStepValue is TaskProduct) {
                        Timber.d("Ensuring lastTaskProduct from previous step $prevStepId")
                        contextResults["lastTaskProduct"] = prevStepValue
                        contextResults["lastProduct"] = prevStepValue.product
                    } else if (prevStepValue is Product) {
                        Timber.d("Ensuring lastProduct from previous step $prevStepId")
                        contextResults["lastProduct"] = prevStepValue
                    }
                }
            }
        }

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = stepId,
            results = contextResults, // Используем обогащенный контекст
            hasStepResult = hasResult,
            onUpdate = { },
            onComplete = { result ->
                Timber.d("ActionContext.onComplete called with result: ${result?.javaClass?.simpleName}")
                if (result != null) {
                    onStepComplete(result)
                } else {
                    onBack()
                }
            },
            onBack = {
                Timber.d("ActionContext.onBack called")
                onBack()
            },
            onForward = {
                Timber.d("ActionContext.onForward called")
                onForward()
            },
            onSkip = { result ->
                Timber.d("ActionContext.onSkip called with result: ${result?.javaClass?.simpleName}")
                onSkip(result)
            },
            onCancel = {
                Timber.d("ActionContext.onCancel called")
                onCancel()
            },
            lastScannedBarcode = lastScannedBarcode,
            validationError = validationError,
            // Передаем флаг глобального состояния для обработки в UI
            isProcessingStep = state.isProcessingStep,
            isFirstStep = state.currentStepIndex == 0
        )
    }
}