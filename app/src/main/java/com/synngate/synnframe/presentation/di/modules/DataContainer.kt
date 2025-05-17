package com.synngate.synnframe.presentation.di.modules

import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.local.database.dao.LogDao
import com.synngate.synnframe.data.local.database.dao.ProductDao
import com.synngate.synnframe.data.local.database.dao.ServerDao
import com.synngate.synnframe.data.local.database.dao.UserDao
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.DataAPI
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
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
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    // DAO
    override val serverDao: ServerDao by lazy {
        database.serverDao()
    }

    override val userDao: UserDao by lazy {
        database.userDao()
    }

    override val logDao: LogDao by lazy {
        database.logDao()
    }

    override val productDao: ProductDao by lazy {
        database.productDao()
    }

    // Репозитории
    override val serverRepository: ServerRepository by lazy {
        Timber.d("Creating ServerRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val userRepository: UserRepository by lazy {
        Timber.d("Creating UserRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val productRepository: ProductRepository by lazy {
        Timber.d("Creating ProductRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val logRepository: LogRepository by lazy {
        Timber.d("Creating LogRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val settingsRepository: SettingsRepository by lazy {
        Timber.d("Creating SettingsRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val dynamicMenuRepository: DynamicMenuRepository by lazy {
        Timber.d("Creating DynamicMenuRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val taskXRepository: TaskXRepository by lazy {
        Timber.d("Creating TaskXRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    // Временно используем Any до замены на правильные типы
    override val wizardBinRepository: Any by lazy {
        Timber.d("Creating WizardBinRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val wizardPalletRepository: Any by lazy {
        Timber.d("Creating WizardPalletRepository")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Data module initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up Data module")
    }
}