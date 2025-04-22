package com.synngate.synnframe.presentation.ui.operation.model

sealed class OperationTaskDetailEvent {
    data object NavigateBack : OperationTaskDetailEvent()
    data class ShowSnackbar(val message: String) : OperationTaskDetailEvent()
    data object StartTaskExecution : OperationTaskDetailEvent()
}