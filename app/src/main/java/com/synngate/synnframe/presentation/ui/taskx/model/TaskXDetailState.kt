package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX

enum class TaskXDetailView {
    PLANNED_ACTIONS,  // Запланированные действия
    FACT_ACTIONS      // Фактические действия
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
    val statusActions: List<StatusActionData> = emptyList()
)

sealed class TaskXDetailEvent {
    data class ShowSnackbar(val message: String) : TaskXDetailEvent()
    object ShowActionWizard : TaskXDetailEvent()
    object HideActionWizard : TaskXDetailEvent()
}

// Вспомогательные классы для UI
data class StatusActionData(
    val id: String,
    val iconName: String,    // Имя иконки для сопоставления в UI
    val text: String,
    val description: String,
    val onClick: () -> Unit
)