package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.operation.OperationTask

data class OperationTaskDetailState(
    val task: OperationTask? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)