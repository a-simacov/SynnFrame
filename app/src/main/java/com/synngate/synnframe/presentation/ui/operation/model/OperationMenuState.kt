package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.operation.OperationMenuItem

data class OperationMenuState(
    val operations: List<OperationMenuItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)