package com.synngate.synnframe.presentation.ui.operation.model

sealed class OperationTasksEvent {

    data object NavigateBack : OperationTasksEvent()

    data class ShowSnackbar(val message: String) : OperationTasksEvent()
}