package com.synngate.synnframe.presentation.ui.login.model

/**
 * Состояние экрана логина
 */
data class LoginState(
    // Пароль, введенный пользователем
    val password: String = "",

    // Флаг процесса аутентификации
    val isLoading: Boolean = false,

    // Сообщение об ошибке (если есть)
    val error: String? = null,

    // Флаг видимости диалога подтверждения выхода
    val showExitConfirmation: Boolean = false,

    // Флаг, указывающий, настроен ли активный сервер
    val hasActiveServer: Boolean = false
)

/**
 * События для экрана логина
 */
sealed class LoginEvent {
    /**
     * Навигация на экран главного меню после успешной аутентификации
     */
    data object NavigateToMainMenu : LoginEvent()

    /**
     * Навигация на экран списка серверов
     * (если нет активного сервера)
     */
    data object NavigateToServerList : LoginEvent()

    /**
     * Показать сообщение в снекбаре
     */
    data class ShowSnackbar(val message: String) : LoginEvent()

    /**
     * Показать диалог подтверждения выхода
     */
    data object ShowExitConfirmation : LoginEvent()

    /**
     * Выход из приложения
     */
    data object ExitApp : LoginEvent()
}