package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

enum class TaskXDetailView {
    PLANNED_ACTIONS,
    FACT_ACTIONS
}

data class TaskXDetailState(
    val task: TaskX? = null,
    val taskType: TaskTypeX? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val activeView: TaskXDetailView = TaskXDetailView.PLANNED_ACTIONS,
    val showVerificationDialog: Boolean = false,
    val currentUserId: String? = null,
    val showCompletionDialog: Boolean = false,
    val showActionWizard: Boolean = false,
    val showOrderRequiredMessage: Boolean = false,
    val nextActionId: String? = null,
    val hasAdditionalActions: Boolean = false,
    val hasPartiallyCompletedActions: Boolean = false,
    val statusActions: List<StatusActionData> = emptyList(),
    val actionsDisplayMode: ActionDisplayMode = ActionDisplayMode.CURRENT,
    val filteredActions: List<PlannedAction> = emptyList()
)

sealed class TaskXDetailEvent {
    data class ShowSnackbar(val message: String) : TaskXDetailEvent()
    object ShowActionWizard : TaskXDetailEvent()
    object HideActionWizard : TaskXDetailEvent()
}

data class StatusActionData(
    val id: String,
    val iconName: String,
    val text: String,
    val description: String,
    val onClick: () -> Unit
)