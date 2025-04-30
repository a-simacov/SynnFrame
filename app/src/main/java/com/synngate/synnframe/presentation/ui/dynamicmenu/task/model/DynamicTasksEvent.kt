package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

sealed class DynamicTasksEvent {
    data object NavigateBack : DynamicTasksEvent()
    data class ShowSnackbar(val message: String) : DynamicTasksEvent()

    // Обновление: теперь передаем taskId и endpoint вместо объекта DynamicTask
    data class NavigateToTaskDetail(val taskId: String, val endpoint: String) : DynamicTasksEvent()

    data class NavigateToTaskXDetail(val taskId: String) : DynamicTasksEvent()
}