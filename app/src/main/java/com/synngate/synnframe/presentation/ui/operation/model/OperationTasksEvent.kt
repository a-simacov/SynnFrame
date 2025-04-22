package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.operation.OperationTask

sealed class OperationTasksEvent {
    data object NavigateBack : OperationTasksEvent()
    data class ShowSnackbar(val message: String) : OperationTasksEvent()
    data class NavigateToTaskDetail(val task: OperationTask) : OperationTasksEvent()
}