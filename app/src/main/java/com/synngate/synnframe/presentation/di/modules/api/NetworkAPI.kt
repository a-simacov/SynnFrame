package com.synngate.synnframe.presentation.di.modules.api

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.data.remote.api.ValidationApiServiceImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.util.network.NetworkMonitor
import io.ktor.client.HttpClient

/**
 * Интерфейс для предоставления сетевых компонентов.
 * Включает HTTP-клиент, различные API-сервисы и компоненты для работы с сетью.
 */
interface NetworkAPI {
    /**
     * HTTP-клиент для выполнения сетевых запросов
     */
    val httpClient: HttpClient

    /**
     * Провайдер для доступа к активному серверу
     */
    val serverProvider: ServerProvider

    /**
     * Монитор состояния сети
     */
    val networkMonitor: NetworkMonitor

    /**
     * Базовый API-сервис
     */
    val apiService: ApiService

    /**
     * API для аутентификации пользователей
     */
    val authApi: AuthApi

    /**
     * API для работы с продуктами
     */
    val productApi: ProductApi

    /**
     * API для обновления приложения
     */
    val appUpdateApi: AppUpdateApi

    /**
     * API для работы с динамическим меню
     */
    val dynamicMenuApi: DynamicMenuApi

    /**
     * Сервис для валидации данных через API
     */
    val validationApiService: ValidationApiServiceImpl

    /**
     * API для поиска действий
     */
    val actionSearchApi: ActionSearchApi

    /**
     * API для работы с заданиями типа X
     */
    val taskXApi: TaskXApi
}