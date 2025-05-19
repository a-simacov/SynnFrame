package com.synngate.synnframe.domain.model.wizard

data class WizardCallbacks(
    val onUpdate: (Map<String, Any>) -> Unit,
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit,
    val onForward: () -> Unit,
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit
)

class ActionContextHolder(
    val taskId: String,
    val actionId: String,
    private val callbacks: WizardCallbacks
) {
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

    fun getContextForStep(
        stepId: String,
        results: Map<String, Any>,
        hasStepResult: Boolean,
        validationError: String? = null,
        lastScannedBarcode: String? = null,
        isProcessingStep: Boolean = false,
        isFirstStep: Boolean = false
    ): ActionContext {

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