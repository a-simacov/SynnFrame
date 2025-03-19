package com.synngate.synnframe.presentation.ui.main.model

import com.synngate.synnframe.domain.entity.User

/**
 * Состояние экрана главного меню
 */
data class MainMenuState(
    // Информация о текущем пользователе
    val currentUser: User? = null,

    // Количество назначенных заданий
    val assignedTasksCount: Int = 0,

    // Общее количество товаров
    val totalProductsCount: Int = 0,

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Сообщение об ошибке (если есть)
    val error: String? = null,

    // Флаг отображения диалога подтверждения выхода
    val showExitConfirmation: Boolean = false,

    // Признак синхронизации с сервером
    val isSyncing: Boolean = false,

    // Время последней синхронизации
    val lastSyncTime: String? = null
)

/**
 * События для экрана главного меню
 */
sealed class MainMenuEvent {
    /**
     * Навигация к экрану списка заданий
     */
    data object NavigateToTasks : MainMenuEvent()

    /**
     * Навигация к экрану списка товаров
     */
    data object NavigateToProducts : MainMenuEvent()

    /**
     * Навигация к экрану логов
     */
    data object NavigateToLogs : MainMenuEvent()

    /**
     * Навигация к экрану настроек
     */
    data object NavigateToSettings : MainMenuEvent()

    /**
     * Навигация к экрану логина (смена пользователя)
     */
    data object NavigateToLogin : MainMenuEvent()

    /**
     * Показать диалог подтверждения выхода
     */
    data object ShowExitConfirmation : MainMenuEvent()

    /**
     * Выход из приложения
     */
    data object ExitApp : MainMenuEvent()

    /**
     * Показать сообщение в снекбаре
     */
    data class ShowSnackbar(val message: String) : MainMenuEvent()
}