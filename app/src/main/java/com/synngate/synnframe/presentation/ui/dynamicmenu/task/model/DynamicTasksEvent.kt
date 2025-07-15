package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

sealed class DynamicTasksEvent {

    data object NavigateBack : DynamicTasksEvent()

    data object RefreshTaskList : DynamicTasksEvent()

    data class ShowSnackbar(val message: String) : DynamicTasksEvent()

    data class NavigateToTaskDetail(val taskId: String, val endpoint: String) : DynamicTasksEvent()

    // Обновленное событие с endpoint
    data class NavigateToTaskXDetail(val taskId: String, val endpoint: String) : DynamicTasksEvent()
}