package com.synngate.synnframe.presentation.ui.operation.model

sealed class OperationMenuEvent {
    data class NavigateToOperationTasks(val operationId: String, val operationName: String) : OperationMenuEvent()

    data object NavigateBack : OperationMenuEvent()

    data class ShowSnackbar(val message: String) : OperationMenuEvent()
}