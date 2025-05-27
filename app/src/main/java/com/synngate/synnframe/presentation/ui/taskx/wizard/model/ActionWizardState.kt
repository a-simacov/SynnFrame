package com.synngate.synnframe.presentation.ui.taskx.wizard.model

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate

data class ActionWizardState(
    val taskId: String = "",
    val actionId: String = "",
    val currentStepIndex: Int = 0,
    val steps: List<ActionStepTemplate> = emptyList(),
    val plannedAction: PlannedAction? = null,
    val factAction: FactAction? = null,
    val selectedObjects: Map<String, Any> = emptyMap(), // key: stepId, value: выбранный объект
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSummary: Boolean = false,
    val showExitDialog: Boolean = false,
    val sendingFailed: Boolean = false // Флаг ошибки отправки на сервер
)