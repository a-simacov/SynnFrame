package com.synngate.synnframe.presentation.ui.logs.model

sealed class LogDetailUiEvent {

    data class ShowSnackbar(val message: String) : LogDetailUiEvent()

    data object NavigateBack : LogDetailUiEvent()
}

sealed class LogDetailStateEvent {

    data object ShowDeleteConfirmation : LogDetailStateEvent()

    data object HideDeleteConfirmation : LogDetailStateEvent()
}