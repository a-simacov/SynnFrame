package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import java.time.LocalDateTime

data class ActionWizardState(
    val taskId: String = "",
    val actionId: String = "",
    val action: PlannedAction? = null,
    val steps: List<WizardStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val results: Map<String, Any> = emptyMap(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val errors: Map<String, String> = emptyMap(),
    val isInitialized: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isProcessingStep: Boolean = false,
    val isSending: Boolean = false,
    val sendError: String? = null
) {

    val currentStep: WizardStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    val canGoBack: Boolean
        get() = currentStepIndex > 0 && currentStep?.canNavigateBack ?: true

    fun getCurrentStepResult(): Any? {
        return currentStep?.let { results[it.id] }
    }

    fun hasResultForStep(stepId: String): Boolean {
        return results.containsKey(stepId)
    }

    fun hasErrorForStep(stepId: String): Boolean {
        return errors.containsKey(stepId)
    }

    fun getErrorForStep(stepId: String): String? {
        return errors[stepId]
    }
}