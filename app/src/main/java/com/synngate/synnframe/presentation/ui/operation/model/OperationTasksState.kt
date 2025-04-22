package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.OperationMenuType
import com.synngate.synnframe.domain.entity.operation.OperationTask

data class OperationTasksState(
    val operationId: String = "",
    val operationName: String = "",
    val operationType: OperationMenuType = OperationMenuType.SHOW_LIST,
    val tasks: List<OperationTask> = emptyList(),
    val searchValue: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundTask: OperationTask? = null
)