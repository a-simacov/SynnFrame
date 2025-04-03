package com.synngate.synnframe.presentation.ui.logs.model

sealed class LogListUiEvent {
    data class ShowSnackbar(val message: String) : LogListUiEvent()
    data class NavigateToLogDetail(val logId: Int) : LogListUiEvent()
}

sealed class LogListStateEvent {
    data object ShowDeleteAllConfirmation : LogListStateEvent()
    data object HideDeleteAllConfirmation : LogListStateEvent()
    data object ShowCleanupDialog : LogListStateEvent()
    data object HideCleanupDialog : LogListStateEvent()
    data object ShowDateFilterDialog : LogListStateEvent()
    data object HideDateFilterDialog : LogListStateEvent()
    data class CleanupOldLogs(val days: Int) : LogListStateEvent()
    data object DeleteAllLogs : LogListStateEvent()
}