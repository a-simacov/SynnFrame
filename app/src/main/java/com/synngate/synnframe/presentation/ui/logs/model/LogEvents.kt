package com.synngate.synnframe.presentation.ui.logs.model

sealed class LogListEvent {

    data class ShowSnackbar(val message: String) : LogListEvent()

    data class NavigateToLogDetail(val logId: Int) : LogListEvent()

    data object ShowDeleteAllConfirmation : LogListEvent()
}

sealed class LogDetailEvent {

    data class ShowSnackbar(val message: String) : LogDetailEvent()

    data object NavigateBack : LogDetailEvent()

    data object ShowDeleteConfirmation : LogDetailEvent()

    data object HideDeleteConfirmation : LogDetailEvent()
}