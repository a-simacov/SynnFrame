package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import java.time.LocalDateTime

data class TaskXListState(
    val tasks: List<TaskX> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null,

    val tasksCount: Int = 0,

    val hasActiveFilters: Boolean = false,

    val dateFromFilter: LocalDateTime? = null,

    val dateToFilter: LocalDateTime? = null,

    val searchQuery: String = "",

    val selectedStatuses: Set<TaskXStatus> = emptySet(),

    val selectedTypes: Set<String> = emptySet(),

    val isDateFilterDialogVisible: Boolean = false
)