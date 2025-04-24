package com.synngate.synnframe.presentation.ui.dynamicmenu.model

sealed class DynamicTaskDetailEvent {
    data object NavigateBack : DynamicTaskDetailEvent()
    data class ShowSnackbar(val message: String) : DynamicTaskDetailEvent()
    data object StartTaskExecution : DynamicTaskDetailEvent()
}