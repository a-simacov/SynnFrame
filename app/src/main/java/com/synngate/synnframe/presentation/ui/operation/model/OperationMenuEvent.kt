package com.synngate.synnframe.presentation.ui.operation.model

import com.synngate.synnframe.domain.entity.OperationMenuType

sealed class OperationMenuEvent {
    data class NavigateToOperationTasks(
        val operationId: String,
        val operationName: String,
        val operationType: OperationMenuType
    ) : OperationMenuEvent()

    data object NavigateBack : OperationMenuEvent()

    data class ShowSnackbar(val message: String) : OperationMenuEvent()
}