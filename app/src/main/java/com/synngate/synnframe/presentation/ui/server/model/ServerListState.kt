package com.synngate.synnframe.presentation.ui.server.model

import com.synngate.synnframe.domain.entity.Server

/**
 * Состояние экрана списка серверов
 */
data class ServerListState(
    // Список серверов для отображения
    val servers: List<Server> = emptyList(),

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Ошибка (если есть)
    val error: String? = null,

    // Настройка отображения списка серверов при запуске
    val showServersOnStartup: Boolean = true,

    // Идентификатор активного сервера (если есть)
    val activeServerId: Int? = null
)

/**
 * События для экрана списка серверов
 */
sealed class ServerListEvent {
    // Навигация к экрану деталей сервера
    data class NavigateToServerDetail(val serverId: Int? = null) : ServerListEvent()

    // Навигация к экрану логина
    data object NavigateToLogin : ServerListEvent()

    // Показать снэкбар с сообщением
    data class ShowSnackbar(val message: String) : ServerListEvent()

    // Показать диалог подтверждения удаления
    data class ShowDeleteConfirmation(val serverId: Int, val serverName: String) : ServerListEvent()
}