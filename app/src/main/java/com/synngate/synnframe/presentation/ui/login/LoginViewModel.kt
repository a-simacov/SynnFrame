package com.synngate.synnframe.presentation.ui.login

import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.login.model.LoginEvent
import com.synngate.synnframe.presentation.ui.login.model.LoginState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * ViewModel для экрана логина
 */
class LoginViewModel(
    private val userUseCases: UserUseCases,
    private val serverUseCases: ServerUseCases,
    private val deviceInfoService: DeviceInfoService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<LoginState, LoginEvent>(LoginState()) {

    init {
        checkActiveServer()
    }

    /**
     * Проверка наличия активного сервера
     */
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

    /**
     * Обновление пароля при вводе пользователем
     */
    fun updatePassword(password: String) {
        updateState { it.copy(password = password, error = null) }
    }

    /**
     * Очистка пароля
     */
    fun clearPassword() {
        updateState { it.copy(password = "", error = null) }
    }

    /**
     * Очистка ошибки
     */
    fun clearError() {
        updateState { it.copy(error = null) }
    }

    /**
     * Обработка нажатия на кнопку "Логин"
     */
    fun login() {
        val state = uiState.value
        val password = state.password.trim()

        // Валидация пароля
        if (password.isEmpty()) {
            updateState { it.copy(error = "Введите пароль") }
            return
        }

        // Проверяем наличие активного сервера
        if (!state.hasActiveServer) {
            sendEvent(LoginEvent.NavigateToServerList)
            return
        }

        // Выполняем аутентификацию
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // Получаем информацию об устройстве для аутентификации
                val deviceInfo = deviceInfoService.getDeviceInfo()

                // Выполняем аутентификацию через UseCase
                val result = userUseCases.loginUser(password, deviceInfo)

                if (result.isSuccess) {
                    // Успешная аутентификация
                    updateState { it.copy(isLoading = false, error = null) }
                    sendEvent(LoginEvent.NavigateToMainMenu)
                } else {
                    // Ошибка аутентификации
                    val errorMessage = result.exceptionOrNull()?.message ?: "Ошибка аутентификации"
                    updateState { it.copy(isLoading = false, error = errorMessage) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during login")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Неизвестная ошибка аутентификации"
                    )
                }
            }
        }
    }

    /**
     * Показать диалог подтверждения выхода
     */
    fun showExitConfirmation() {
        updateState { it.copy(showExitConfirmation = true) }
        sendEvent(LoginEvent.ShowExitConfirmation)
    }

    /**
     * Скрыть диалог подтверждения выхода
     */
    fun hideExitConfirmation() {
        updateState { it.copy(showExitConfirmation = false) }
    }

    /**
     * Выход из приложения
     */
    fun exitApp() {
        sendEvent(LoginEvent.ExitApp)
    }

    /**
     * Навигация к экрану списка серверов
     */
    fun navigateToServerList() {
        sendEvent(LoginEvent.NavigateToServerList)
    }
}