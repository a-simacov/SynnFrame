package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import timber.log.Timber

/**
 * Фабрика для создания контекста действий визарда
 */
class ActionWizardContextFactory {

    /**
     * Создает контекст для шага действия
     *
     * @param state Состояние визарда
     * @param onStepComplete Обработчик завершения шага
     * @param onBack Обработчик навигации назад
     * @param onForward Обработчик перехода вперед
     * @param onSkip Обработчик пропуска шага
     * @param onCancel Обработчик отмены визарда
     * @param lastScannedBarcode Последний отсканированный штрихкод (опционально)
     * @return Контекст для шага
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
        val currentStep = state.currentStep
        if (currentStep == null) {
            Timber.w("Creating context for null step")
        }

        // Проверяем, есть ли результат для текущего шага
        val stepId = currentStep?.id ?: ""
        val hasResult = state.results.containsKey(stepId)

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = stepId,
            results = state.results,
            hasStepResult = hasResult,
            onUpdate = { updatedResults ->
                // Этот метод вызывается для обновления промежуточных результатов,
                // но пока не используется
                Timber.d("Update results callback called, but not implemented")
            },
            onComplete = { result ->
                if (result != null) {
                    Timber.d("Step completed with result: $result")
                    onStepComplete(result)
                } else {
                    Timber.d("Step completed with null result, going back")
                    onBack()
                }
            },
            onBack = {
                Timber.d("Back navigation requested")
                onBack()
            },
            onForward = {
                Timber.d("Forward navigation requested")
                onForward()
            },
            onSkip = { result ->
                Timber.d("Skip requested with result: $result")
                onSkip(result)
            },
            onCancel = {
                Timber.d("Cancel requested")
                onCancel()
            },
            lastScannedBarcode = lastScannedBarcode
        )
    }
}