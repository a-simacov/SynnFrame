package com.synngate.synnframe.presentation.ui.operation

import com.synngate.synnframe.domain.entity.operation.OperationTask
import com.synngate.synnframe.presentation.ui.operation.model.OperationTaskDetailEvent
import com.synngate.synnframe.presentation.ui.operation.model.OperationTaskDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel

class OperationTaskDetailViewModel(
    val task: OperationTask
) : BaseViewModel<OperationTaskDetailState, OperationTaskDetailEvent>(
    OperationTaskDetailState(task = task)
) {

    fun onStartTaskExecution() {
        // В будущем будет реализована логика запуска задания
        sendEvent(OperationTaskDetailEvent.ShowSnackbar("Начало работы с заданием будет реализовано позже"))
        sendEvent(OperationTaskDetailEvent.StartTaskExecution)
    }

    fun onBackClick() {
        sendEvent(OperationTaskDetailEvent.NavigateBack)
    }
}