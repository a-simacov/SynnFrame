package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.SavableObject
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
    val isProcessingDialogAction: Boolean = false,
    val error: String? = null,
    val activeView: TaskXDetailView = TaskXDetailView.PLANNED_ACTIONS,
    val showVerificationDialog: Boolean = false,
    val currentUserId: String? = null,
    val showCompletionDialog: Boolean = false,
    val showOrderRequiredMessage: Boolean = false,
    val nextActionId: String? = null,
    val hasAdditionalActions: Boolean = false,
    val statusActions: List<StatusActionData> = emptyList(),
    val actionsDisplayMode: ActionDisplayMode = ActionDisplayMode.CURRENT,
    val filteredActions: List<PlannedAction> = emptyList(),
    val showActionsDialog: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val showSearchField: Boolean = false,
    val showCameraScannerForSearch: Boolean = false,
    val filteredActionIds: List<String> = emptyList(),

    // Поля для поддержки начальных действий
    val hasInitialActions: Boolean = false,
    val areInitialActionsCompleted: Boolean = false,
    val completedInitialActionsCount: Int = 0,
    val totalInitialActionsCount: Int = 0,
    val initialActionsIds: List<String> = emptyList(),
    val showInitialActionsRequiredDialog: Boolean = false,

    // Поля для поддержки сохраняемых объектов
    val savableObjects: List<SavableObject> = emptyList(),
    val showSavableObjectsPanel: Boolean = false,
    val supportsSavableObjects: Boolean = false,

    // Новые поля для улучшения UX при фильтрации
    val searchInfo: String = "",                         // Информация о результатах поиска
    val isFilteredBySavableObjects: Boolean = false,     // Признак фильтрации по сохраняемым объектам
    val activeFiltersCount: Int = 0,                     // Количество активных фильтров
    val filterMessage: String = ""                       // Сообщение о применяемых фильтрах
)

sealed class TaskXDetailEvent {
    data class ShowSnackbar(val message: String) : TaskXDetailEvent()
    data class NavigateToActionWizard(val taskId: String, val actionId: String) : TaskXDetailEvent()
    object NavigateBack : TaskXDetailEvent()

    // Новое событие, которое сразу включает и сообщение, и навигацию назад
    data class TaskActionCompleted(val message: String) : TaskXDetailEvent()
}

data class StatusActionData(
    val id: String,
    val iconName: String,
    val text: String,
    val description: String,
    val onClick: () -> Unit
)