package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX

sealed class DynamicTasksEvent {

    data object NavigateBack : DynamicTasksEvent()

    data class ShowSnackbar(val message: String) : DynamicTasksEvent()

    data class NavigateToTaskDetail(val taskId: String, val endpoint: String) : DynamicTasksEvent()

    data class NavigateToTaskXDetail(val taskId: String) : DynamicTasksEvent()

    data class SetTaskDataAndNavigate(
        val task: TaskX,
        val taskType: TaskTypeX,
        val endpoint: String
    ) : DynamicTasksEvent()
}