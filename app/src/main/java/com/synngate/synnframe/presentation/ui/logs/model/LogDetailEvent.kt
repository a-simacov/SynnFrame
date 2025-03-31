package com.synngate.synnframe.presentation.ui.logs.model

sealed class LogDetailEvent {

    data class ShowSnackbar(val message: String) : LogDetailEvent()

    data object NavigateBack : LogDetailEvent()

    data object ShowDeleteConfirmation : LogDetailEvent()

    data object HideDeleteConfirmation : LogDetailEvent()
}