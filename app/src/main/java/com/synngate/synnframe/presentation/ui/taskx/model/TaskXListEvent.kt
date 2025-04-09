package com.synngate.synnframe.presentation.ui.taskx.model

sealed class TaskXListEvent {
    data class NavigateToTaskDetail(val taskId: String) : TaskXListEvent()
    data class ShowSnackbar(val message: String) : TaskXListEvent()
}