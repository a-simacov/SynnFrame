package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.buffer.BufferItem

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

    // Поля для поддержки буфера задания
    val bufferItems: List<BufferItem> = emptyList(),
    val showBufferPanel: Boolean = false,
    val supportsTaskBuffer: Boolean = false,

    // Новые поля для улучшения UX при фильтрации
    val searchInfo: String = "",
    val isFilteredByBuffer: Boolean = false,
    val activeFiltersCount: Int = 0,
    val filterMessage: String = "",

    // Признак, что задание с ручным завершением действий
    val supportsManualActionCompletion: Boolean = false
) {
    fun getDisplayActions(): List<PlannedAction> {
        return when {
            filteredActionIds.isNotEmpty() -> {
                task?.plannedActions?.filter { it.id in filteredActionIds } ?: emptyList()
            }
            isFilteredByBuffer -> filteredActions
            else -> {
                when (actionsDisplayMode) {
                    ActionDisplayMode.CURRENT -> {
                        task?.plannedActions?.filter {
                            !it.isCompleted && !it.manuallyCompleted && !it.isSkipped
                        } ?: emptyList()
                    }
                    ActionDisplayMode.COMPLETED -> {
                        task?.plannedActions?.filter {
                            it.isCompleted || it.manuallyCompleted
                        } ?: emptyList()
                    }
                    ActionDisplayMode.ALL -> task?.plannedActions ?: emptyList()
                    ActionDisplayMode.INITIALS -> task?.getInitialActions() ?: emptyList()
                    ActionDisplayMode.FINALS -> task?.getFinalActions() ?: emptyList()
                }
            }
        }
    }
}

data class StatusActionData(
    val id: String,
    val iconName: String,
    val text: String,
    val description: String,
    val onClick: () -> Unit
)