package com.synngate.synnframe.presentation.ui.logs.model

import com.synngate.synnframe.presentation.viewmodel.StateEventHandler

sealed class LogListUiEvent {
    data class ShowSnackbar(val message: String) : LogListUiEvent()
    data class NavigateToLogDetail(val logId: Int) : LogListUiEvent()
}

sealed class LogListStateEvent : StateEventHandler<LogListState> {

    data object ShowDeleteAllConfirmation : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isDeleteAllConfirmationVisible = true)
        }
    }

    data object HideDeleteAllConfirmation : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isDeleteAllConfirmationVisible = false)
        }
    }

    data object ShowCleanupDialog : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isCleanupDialogVisible = true)
        }
    }

    data object HideCleanupDialog : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isCleanupDialogVisible = false)
        }
    }

    data object ShowDateFilterDialog : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isDateFilterDialogVisible = true)
        }
    }

    data object HideDateFilterDialog : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            return currentState.copy(isDateFilterDialogVisible = false)
        }
    }

    /**
     * События, требующие дополнительных действий помимо изменения состояния
     */
    data class CleanupOldLogs(val days: Int) : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            // Устанавливаем состояние загрузки
            return currentState.copy(isLoading = true, error = null)
        }
    }

    data object DeleteAllLogs : LogListStateEvent() {
        override fun handle(currentState: LogListState): LogListState {
            // Устанавливаем состояние загрузки
            return currentState.copy(isLoading = true, error = null)
        }
    }
}