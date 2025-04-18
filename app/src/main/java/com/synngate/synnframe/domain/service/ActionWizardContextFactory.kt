package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.model.wizard.ActionWizardState

/**
 * Фабрика для создания контекста шага визарда
 */
class ActionWizardContextFactory {

    /**
     * Создает контекст для шага визарда
     * @param state Состояние визарда
     * @param onStepComplete Обработчик завершения шага
     * @param onBack Обработчик возврата
     * @param onSkip Обработчик пропуска шага
     * @param onCancel Обработчик отмены
     */
    fun createContext(
        state: ActionWizardState,
        onStepComplete: (Any?) -> Unit,
        onBack: () -> Unit,  // Изменено для соответствия типу handleBack
        onSkip: (Any?) -> Unit,
        onCancel: () -> Unit
    ): ActionContext {
        val currentStep = state.currentStep ?: throw IllegalStateException("No current step")

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = currentStep.id,
            results = state.results,
            onUpdate = { updatedResults ->
                // Создаем копию результатов и добавляем новые
                val newResults = state.results.toMutableMap()
                newResults.putAll(updatedResults)

                // Уведомляем о завершении шага с обновленными результатами
                onStepComplete(newResults)
            },
            onComplete = onStepComplete,
            onBack = onBack,
            onSkip = onSkip,
            onCancel = onCancel
        )
    }
}