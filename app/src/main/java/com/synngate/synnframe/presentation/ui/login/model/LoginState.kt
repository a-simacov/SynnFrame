package com.synngate.synnframe.presentation.ui.login.model

data class LoginState(

    val password: String = "",

    val isLoading: Boolean = false,

    val error: String? = null,

    val showExitConfirmation: Boolean = false,

    val hasActiveServer: Boolean = false
)

sealed class LoginEvent {

    data object NavigateToMainMenu : LoginEvent()

    data object NavigateToServerList : LoginEvent()

    data class ShowSnackbar(val message: String) : LoginEvent()

    data object ShowExitConfirmation : LoginEvent()

    data object ExitApp : LoginEvent()
}