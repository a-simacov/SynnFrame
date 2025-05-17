package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.AppUpdateApi
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.data.remote.api.DynamicMenuApi
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.data.remote.api.ValidationApiServiceImpl
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
import com.synngate.synnframe.presentation.di.modules.api.NetworkAPI
import com.synngate.synnframe.util.network.NetworkMonitor
import io.ktor.client.HttpClient
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

    override val httpClient: HttpClient by lazy {
        Timber.d("Creating HttpClient")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val serverProvider: ServerProvider by lazy {
        Timber.d("Creating ServerProvider")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val networkMonitor: NetworkMonitor by lazy {
        Timber.d("Creating NetworkMonitor")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val apiService: ApiService by lazy {
        Timber.d("Creating ApiService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val authApi: AuthApi by lazy {
        Timber.d("Creating AuthApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val productApi: ProductApi by lazy {
        Timber.d("Creating ProductApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val appUpdateApi: AppUpdateApi by lazy {
        Timber.d("Creating AppUpdateApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val dynamicMenuApi: DynamicMenuApi by lazy {
        Timber.d("Creating DynamicMenuApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val validationApiService: ValidationApiServiceImpl by lazy {
        Timber.d("Creating ValidationApiService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val actionSearchApi: ActionSearchApi by lazy {
        Timber.d("Creating ActionSearchApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val taskXApi: TaskXApi by lazy {
        Timber.d("Creating TaskXApi")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Network module initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up Network module")
    }
}