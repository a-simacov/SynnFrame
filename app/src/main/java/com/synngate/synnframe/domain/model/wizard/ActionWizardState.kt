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
    val errors: Map<String, String> = emptyMap(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val isInitialized: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isProcessingStep: Boolean = false,
    val isSending: Boolean = false,
    val sendError: String? = null,

    // Флаг для отслеживания возможности автозаполнения
    val autoFillEnabled: Boolean = false,

    // Информация о шагах, которые могут быть автозаполнены
    val autoFillableSteps: Set<String> = emptySet(),

    // Флаг для отслеживания навигации назад (НОВОЕ)
    val isNavigatingBack: Boolean = false,

    // Флаг для отслеживания того, что текущий шаг был автозаполнен в этой сессии (НОВОЕ)
    val currentStepAutoFilled: Boolean = false
) {
    val currentStep: WizardStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    val canGoBack: Boolean
        get() = currentStepIndex > 0 && currentStep?.canNavigateBack ?: true

    val isUninitialized: Boolean
        get() = !isInitialized && action == null
}