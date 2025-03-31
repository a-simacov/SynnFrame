package com.synngate.synnframe.presentation.ui.logs.model

sealed class LogListEvent {
    data class ShowSnackbar(val message: String) : LogListEvent()
    data class NavigateToLogDetail(val logId: Int) : LogListEvent()
    object ShowDeleteAllConfirmation : LogListEvent()
    object ShowDateFilterDialog : LogListEvent()
    object HideDateFilterDialog : LogListEvent()
    object ShowCleanupDialog : LogListEvent()
}