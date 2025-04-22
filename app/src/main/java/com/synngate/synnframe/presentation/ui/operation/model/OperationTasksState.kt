package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.operation.OperationTask

data class OperationTasksState(
    val operationId: String = "",
    val operationName: String = "",
    val tasks: List<OperationTask> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)