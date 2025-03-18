package com.synngate.synnframe.presentation.ui.server.model

import com.synngate.synnframe.domain.entity.Server

/**
 * Состояние экрана деталей сервера
 */
data class ServerDetailState(
    // Данные сервера
    val server: Server? = null,

    // Идентификатор сервера (null для нового сервера)
    val serverId: Int? = null,

    // Режим редактирования (true) или добавления (false)
    val isEditMode: Boolean = false,

    // Имя сервера
    val name: String = "",

    // Хост сервера
    val host: String = "",

    // Порт сервера
    val port: String = "",

    // Точка подключения к API
    val apiEndpoint: String = "",

    // Логин для доступа к API
    val login: String = "",

    // Пароль для доступа к API
    val password: String = "",

    // Активный сервер или нет
    val isActive: Boolean = false,

    // Статус подключения
    val connectionStatus: String = "Статус: ожидание подключения",

    // Тестирование подключения в процессе
    val isTestingConnection: Boolean = false,

    // Сохранение в процессе
    val isSaving: Boolean = false,

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Ошибка валидации полей
    val validationError: String? = null,

    // Общая ошибка
    val error: String? = null
)

/**
 * События для экрана деталей сервера
 */
sealed class ServerDetailEvent {
    // Навигация назад
    data object NavigateBack : ServerDetailEvent()

    // Показать снэкбар с сообщением
    data class ShowSnackbar(val message: String) : ServerDetailEvent()

    // Успешное сохранение сервера
    data class ServerSaved(val serverId: Int) : ServerDetailEvent()

    // Успешное подключение к серверу
    data object ConnectionSuccess : ServerDetailEvent()

    // Ошибка подключения к серверу
    data class ConnectionError(val message: String) : ServerDetailEvent()
}