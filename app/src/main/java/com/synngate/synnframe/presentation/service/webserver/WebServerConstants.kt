package com.synngate.synnframe.presentation.service.webserver

/**
 * Константы для работы с локальным веб-сервером
 */
object WebServerConstants {
    // Маршруты API
    const val ROUTE_ECHO = "/echo"
    const val ROUTE_TASKS = "/tasks"
    const val ROUTE_PRODUCTS = "/products"
    const val ROUTE_TASK_TYPES = "/task-types"

    // Параметры
    const val PORT_DEFAULT = 8080
    const val AUTH_REALM = "SynnFrame API"
    const val AUTH_SCHEME = "auth-basic"

    // Служебные
    const val SYNC_PREFIX = "webserver-"
    const val DEFAULT_SYNC_DURATION = 1000L

    // Тексты логов
    const val LOG_WEB_SERVER_STARTED = "Локальный веб-сервер запущен на порту %d"
    const val LOG_WEB_SERVER_STOPPED = "Локальный веб-сервер остановлен"
    const val LOG_TASKS_RECEIVED = "Получено %d заданий через локальный веб-сервер. Добавлено новых: %d, обновлено: %d. Время обработки: %dмс"
    const val LOG_PRODUCTS_RECEIVED = "Получено %d товаров через локальный веб-сервер. Добавлено новых: %d, обновлено: %d. Время обработки: %dмс"
    const val LOG_PRODUCTS_FULL_UPDATE = "Полное обновление справочника товаров через локальный веб-сервер: %d товаров"
    const val LOG_TASK_TYPES_RECEIVED = "Получено %d типов заданий через локальный веб-сервер. Время обработки: %dмс"
    const val LOG_ERROR_TASKS = "Ошибка обработки заданий: %s"
    const val LOG_ERROR_PRODUCTS = "Ошибка обработки товаров: %s"
    const val LOG_ERROR_TASK_TYPES = "Ошибка обработки типов заданий: %s"

    // Имена параметров для Intent
    const val EXTRA_PORT = "extra_port"

    // Операции
    const val OPERATION_TASKS_RECEIVED = "Получены задания через локальный веб-сервер"
    const val OPERATION_PRODUCTS_RECEIVED = "Получены товары через локальный веб-сервер"
    const val OPERATION_TASK_TYPES_RECEIVED = "Получены типы заданий через локальный веб-сервер"
    const val OPERATION_DATA_RECEIVED = "Данные получены через локальный веб-сервер"
}