package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.DynamicTask

data class DynamicTaskDetailState(
    val task: DynamicTask? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)