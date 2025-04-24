package com.synngate.synnframe.presentation.ui.dynamicmenu

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTaskDetailEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTaskDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel

class DynamicTaskDetailViewModel(
    val task: DynamicTask
) : BaseViewModel<DynamicTaskDetailState, DynamicTaskDetailEvent>(
    DynamicTaskDetailState(task = task)
) {

    fun onStartTaskExecution() {
        sendEvent(DynamicTaskDetailEvent.ShowSnackbar("Начало работы с заданием будет реализовано позже"))
        sendEvent(DynamicTaskDetailEvent.StartTaskExecution)
    }
}