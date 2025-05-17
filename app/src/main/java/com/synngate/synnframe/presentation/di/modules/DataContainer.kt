package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.data.repository.DynamicMenuRepositoryImpl
import com.synngate.synnframe.data.repository.LogRepositoryImpl
import com.synngate.synnframe.data.repository.ProductRepositoryImpl
import com.synngate.synnframe.data.repository.ServerRepositoryImpl
import com.synngate.synnframe.data.repository.SettingsRepositoryImpl
import com.synngate.synnframe.data.repository.TaskXRepositoryImpl
import com.synngate.synnframe.data.repository.UserRepositoryImpl
import com.synngate.synnframe.data.repository.WizardBinRepositoryImpl
import com.synngate.synnframe.data.repository.WizardPalletRepositoryImpl
import com.synngate.synnframe.data.service.LoggingServiceImpl
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.DataAPI
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Модульный контейнер для компонентов доступа к данным (база данных, DAO, репозитории)
 *
 * @param appContainer Основной контейнер приложения
 * @param coreContainer Контейнер базовых компонентов
 * @param networkContainer Контейнер сетевых компонентов
 */
class DataContainer(
    appContainer: AppContainer,
    private val coreContainer: CoreContainer,
    private val networkContainer: NetworkContainer
) : ModuleContainer(appContainer), DataAPI, ModuleAPI {

    override val moduleName: String = "Data"

    // База данных
    override val database: AppDatabase by lazy {
        Timber.d("Creating AppDatabase")
        AppDatabase.getInstance(appContainer.applicationContext)
    }

    // DAO
    override val serverDao by lazy { database.serverDao() }
    override val userDao by lazy { database.userDao() }
    override val logDao by lazy { database.logDao() }
    override val productDao by lazy { database.productDao() }

    // Репозитории
    override val serverRepository: ServerRepository by lazy {
        Timber.d("Creating ServerRepository")
        ServerRepositoryImpl(serverDao, networkContainer.apiService)
    }

    override val userRepository: UserRepository by lazy {
        Timber.d("Creating UserRepository")
        UserRepositoryImpl(userDao, networkContainer.authApi, coreContainer.appSettingsDataStore)
    }

    override val productRepository: ProductRepository by lazy {
        Timber.d("Creating ProductRepository")
        ProductRepositoryImpl(productDao, networkContainer.productApi, database)
    }

    override val logRepository: LogRepository by lazy {
        Timber.d("Creating LogRepository")
        LogRepositoryImpl(logDao)
    }

    // Обновляем LoggingService в CoreContainer с реальным LogRepository
    private val realLoggingService: LoggingService by lazy {
        Timber.d("Creating real LoggingService")
        // Создаем реальный LoggingService
        val service = LoggingServiceImpl(logRepository)

        // Устанавливаем его как singleton, чтобы была единая точка доступа
        service
    }

    // UI-маппер для продуктов
    val productUiMapper: ProductUiMapper by lazy {
        Timber.d("Creating ProductUiMapper")
        ProductUiMapper(coreContainer.resourceProvider)
    }

    override val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        SettingsRepositoryImpl(
            coreContainer.appSettingsDataStore,
            networkContainer.appUpdateApi
        )
    }

    override val dynamicMenuRepository: DynamicMenuRepository by lazy {
        Timber.d("Creating DynamicMenuRepository")
        DynamicMenuRepositoryImpl(networkContainer.dynamicMenuApi)
    }

    // Этот внедрять в networkContainer.serverProvider
    val serverProviderImpl: ServerProvider by lazy {
        Timber.d("Creating real ServerProvider")
        object : ServerProvider {
            override suspend fun getActiveServer() = serverRepository.getActiveServer().first()
            override suspend fun getCurrentUserId() = userRepository.getCurrentUser().first()?.id
        }
    }

    // Менеджер контекста задач (будет нужен для TaskXRepository)
    private val taskContextManager: TaskContextManager by lazy {
        Timber.d("Creating TaskContextManager")
        TaskContextManager()
    }

    override val taskXRepository: TaskXRepository by lazy {
        Timber.d("Creating TaskXRepository")
        TaskXRepositoryImpl(networkContainer.taskXApi, taskContextManager)
    }

    override val wizardBinRepository by lazy {
        Timber.d("Creating WizardBinRepository")
        WizardBinRepositoryImpl(networkContainer.httpClient, serverProviderImpl)
    }

    override val wizardPalletRepository by lazy {
        Timber.d("Creating WizardPalletRepository")
        WizardPalletRepositoryImpl(networkContainer.httpClient, serverProviderImpl)
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Data module initialized")

        // После инициализации Data модуля, обновляем ServerProvider в NetworkContainer
        networkContainer.updateServerProvider(serverProviderImpl)
    }


    override fun cleanup() {
        Timber.d("Cleaning up Data module")
    }
}