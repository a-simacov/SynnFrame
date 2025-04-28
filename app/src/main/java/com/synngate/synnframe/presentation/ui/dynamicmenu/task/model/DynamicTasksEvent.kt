package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

import com.synngate.synnframe.domain.entity.operation.DynamicTask

sealed class DynamicTasksEvent {
    data object NavigateBack : DynamicTasksEvent()
    data class ShowSnackbar(val message: String) : DynamicTasksEvent()
    data class NavigateToTaskDetail(val task: DynamicTask) : DynamicTasksEvent()
}