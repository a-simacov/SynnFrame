package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import java.time.LocalDateTime

data class TaskListState(
    val tasks: List<Task> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null,

    val searchQuery: String = "",

    val selectedStatusFilters: Set<TaskStatus> = emptySet(),

    val selectedTypeFilters: Set<TaskType> = emptySet(),

    val dateFromFilter: LocalDateTime? = null,

    val dateToFilter: LocalDateTime? = null,

    val isFilterPanelVisible: Boolean = false,

    val isSyncing: Boolean = false,

    val lastSyncTime: String? = null,

    val tasksCount: Int = 0,

    val showTypeMenu: Boolean = false,

    val isProcessing: Boolean = false,
)