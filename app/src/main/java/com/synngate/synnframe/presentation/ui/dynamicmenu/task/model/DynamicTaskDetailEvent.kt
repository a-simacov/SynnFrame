package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

sealed class DynamicTaskDetailEvent {
    data object NavigateBack : DynamicTaskDetailEvent()
    data class ShowSnackbar(val message: String) : DynamicTaskDetailEvent()
    data object StartTaskExecution : DynamicTaskDetailEvent()

    // Обновленное событие с параметром endpoint
    data class NavigateToTaskXDetail(val taskId: String, val endpoint: String) : DynamicTaskDetailEvent()
}