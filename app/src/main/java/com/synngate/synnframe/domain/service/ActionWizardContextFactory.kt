package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.model.wizard.ActionWizardState

class ActionWizardContextFactory {

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

        return ActionContext(
            taskId = state.taskId,
            actionId = state.actionId,
            stepId = stepId,
            results = state.results,
            hasStepResult = hasResult,
            onUpdate = { },
            onComplete = { result ->
                if (result != null) {
                    onStepComplete(result)
                } else {
                    onBack()
                }
            },
            onBack = { onBack() },
            onForward = { onForward() },
            onSkip = { result -> onSkip(result) },
            onCancel = { onCancel() },
            lastScannedBarcode = lastScannedBarcode,
            validationError = validationError
        )
    }
}