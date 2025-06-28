package com.synngate.synnframe.presentation.ui.login

import androidx.lifecycle.SavedStateHandle
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.login.model.LoginEvent
import com.synngate.synnframe.presentation.ui.login.model.LoginState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModelWithSavedState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import timber.log.Timber

class LoginViewModel(
    savedStateHandle: SavedStateHandle,
    private val userUseCases: UserUseCases,
    private val serverUseCases: ServerUseCases,
    private val deviceInfoService: DeviceInfoService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModelWithSavedState<LoginState, LoginEvent>(
    savedStateHandle = savedStateHandle,
    initialState = LoginState(
        password = savedStateHandle.get<String>("password") ?: ""
    ),
    ioDispatcher = ioDispatcher
) {

    init {
        checkActiveServer()
    }

    private fun checkActiveServer() {
        launchIO {
            try {
                val activeServer = serverUseCases.getActiveServer().first()
                updateState { it.copy(hasActiveServer = activeServer != null) }
            } catch (e: Exception) {
                Timber.e(e, "Error checking active server")
                updateState { it.copy(hasActiveServer = false) }
            }
        }
    }

    fun updatePassword(password: String) {
        setSavedState("password", password)
        updateState { it.copy(password = password, error = null) }
    }

    fun clearPassword() {
        setSavedState("password", "")
        updateState { it.copy(password = "", error = null) }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    fun login() {
        val state = uiState.value
        val password = state.password.trim()

        if (password.isEmpty()) {
            updateState { it.copy(error = "Enter password") }
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val deviceInfo = deviceInfoService.getDeviceInfo()

                val result = userUseCases.loginUser(password, deviceInfo)

                if (result.isSuccess) {
                    updateState { it.copy(isLoading = false, error = null) }
                    sendEvent(LoginEvent.NavigateToMainMenu)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Authentication error"
                    updateState { it.copy(isLoading = false, error = errorMessage) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during login")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown authentication error"
                    )
                }
            }
        }
    }

    fun showExitConfirmation() {
        updateState { it.copy(showExitConfirmation = true) }
        sendEvent(LoginEvent.ShowExitConfirmation)
    }

    fun hideExitConfirmation() {
        updateState { it.copy(showExitConfirmation = false) }
    }

    fun exitApp() {
        sendEvent(LoginEvent.ExitApp)
    }
}