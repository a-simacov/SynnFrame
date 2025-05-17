package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.ActionSearchApiImpl
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.data.remote.api.AppUpdateApiImpl
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.data.remote.api.AuthApiImpl
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.data.remote.api.DynamicMenuApiImpl
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.data.remote.api.ProductApiImpl
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.data.remote.api.TaskXApiImpl
import com.synngate.synnframe.data.remote.api.ValidationApiServiceImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ApiServiceImpl
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
import com.synngate.synnframe.presentation.di.modules.api.NetworkAPI
import com.synngate.synnframe.util.network.NetworkMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Модульный контейнер для сетевых компонентов (HTTP-клиент, API-сервисы и т.д.)
 *
 * @param appContainer Основной контейнер приложения
 * @param coreContainer Контейнер базовых компонентов
 */
class NetworkContainer(
    appContainer: AppContainer,
    private val coreContainer: CoreContainer
) : ModuleContainer(appContainer), NetworkAPI, ModuleAPI {

    override val moduleName: String = "Network"

    @OptIn(ExperimentalSerializationApi::class)
    override val httpClient: HttpClient by lazy {
        Timber.d("Creating HttpClient")
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    useArrayPolymorphism = true
                    explicitNulls = false
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.tag("HttpClient").d(message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    override val networkMonitor: NetworkMonitor by lazy {
        Timber.d("Creating NetworkMonitor")
        NetworkMonitor(appContainer.applicationContext)
    }

    // Этот интерфейс будет обновлен позже с реальной реализацией
    private var _serverProvider: ServerProvider = object : ServerProvider {
        override suspend fun getActiveServer() = null
        override suspend fun getCurrentUserId() = null
    }

    // Property getter, который возвращает текущее значение _serverProvider
    override val serverProvider: ServerProvider
        get() = _serverProvider

    // Метод для обновления serverProvider с реальной реализацией
    fun updateServerProvider(newProvider: ServerProvider) {
        Timber.d("Updating ServerProvider with real implementation")
        _serverProvider = newProvider
        Timber.d("ServerProvider successfully updated")
    }

    override val apiService: ApiService by lazy {
        Timber.d("Creating ApiService")
        ApiServiceImpl(httpClient, serverProvider)
    }

    override val authApi: AuthApi by lazy {
        Timber.d("Creating AuthApi")
        AuthApiImpl(apiService)
    }

    override val productApi: ProductApi by lazy {
        Timber.d("Creating ProductApi")
        ProductApiImpl(httpClient, serverProvider)
    }

    override val appUpdateApi: AppUpdateApi by lazy {
        Timber.d("Creating AppUpdateApi")
        AppUpdateApiImpl(httpClient, serverProvider)
    }

    override val dynamicMenuApi: DynamicMenuApi by lazy {
        Timber.d("Creating DynamicMenuApi")
        DynamicMenuApiImpl(httpClient, serverProvider)
    }

    override val validationApiService: ValidationApiServiceImpl by lazy {
        Timber.d("Creating ValidationApiService")
        ValidationApiServiceImpl(httpClient, serverProvider)
    }

    override val actionSearchApi: ActionSearchApi by lazy {
        Timber.d("Creating ActionSearchApi")
        ActionSearchApiImpl(httpClient, serverProvider)
    }

    override val taskXApi: TaskXApi by lazy {
        Timber.d("Creating TaskXApi")
        TaskXApiImpl(httpClient, serverProvider)
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Network module initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up Network module")
    }
}