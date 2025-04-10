package com.synngate.synnframe.presentation.ui.main.model

import com.synngate.synnframe.domain.entity.User

data class MainMenuState(
    val currentUser: User? = null,

    val assignedTasksCount: Int = 0,

    val totalProductsCount: Int = 0,

    val isLoading: Boolean = false,

    val error: String? = null,

    val showExitConfirmation: Boolean = false,

    val isSyncing: Boolean = false,

    val lastSyncTime: String? = null
)

sealed class MainMenuEvent {

    data object NavigateToTasks : MainMenuEvent()

    data object NavigateToProducts : MainMenuEvent()

    data object NavigateToLogs : MainMenuEvent()

    data object NavigateToSettings : MainMenuEvent()

    data object NavigateToLogin : MainMenuEvent()

    data object ShowExitConfirmation : MainMenuEvent()

    data object ExitApp : MainMenuEvent()

    data class ShowSnackbar(val message: String) : MainMenuEvent()

    data object NavigateToTasksX : MainMenuEvent()
}