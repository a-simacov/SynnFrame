package com.synngate.synnframe.presentation.di.modules.api

/**
 * Базовый интерфейс для всех модульных API.
 * Определяет базовые методы, которые должны быть реализованы всеми модульными API.
 */
interface ModuleAPI {
    /**
     * Инициализация модуля
     */
    fun initialize()

    /**
     * Очистка ресурсов модуля
     */
    fun cleanup()
}