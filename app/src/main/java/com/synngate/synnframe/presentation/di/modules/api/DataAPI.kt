package com.synngate.synnframe.presentation.di.modules.api

import com.synngate.synnframe.data.local.dao.LogDao
import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.dao.UserDao
import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.domain.repository.DynamicMenuRepository
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.repository.SettingsRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.repository.UserRepository

/**
 * Интерфейс для предоставления компонентов доступа к данным.
 * Включает базу данных, DAO и репозитории.
 */
interface DataAPI {
    /**
     * База данных приложения
     */
    val database: AppDatabase

    /**
     * DAO для работы с серверами
     */
    val serverDao: ServerDao

    /**
     * DAO для работы с пользователями
     */
    val userDao: UserDao

    /**
     * DAO для работы с логами
     */
    val logDao: LogDao

    /**
     * DAO для работы с продуктами
     */
    val productDao: ProductDao

    /**
     * Репозиторий для работы с серверами
     */
    val serverRepository: ServerRepository

    /**
     * Репозиторий для работы с пользователями
     */
    val userRepository: UserRepository

    /**
     * Репозиторий для работы с продуктами
     */
    val productRepository: ProductRepository

    /**
     * Репозиторий для работы с логами
     */
    val logRepository: LogRepository

    /**
     * Репозиторий для работы с настройками
     */
    val settingsRepository: SettingsRepository

    /**
     * Репозиторий для работы с динамическим меню
     */
    val dynamicMenuRepository: DynamicMenuRepository

    /**
     * Репозиторий для работы с заданиями X
     */
    val taskXRepository: TaskXRepository

    /**
     * Репозиторий для работы с ячейками в визарде
     */
    val wizardBinRepository: Any  // Заменить на правильный тип

    /**
     * Репозиторий для работы с паллетами в визарде
     */
    val wizardPalletRepository: Any  // Заменить на правильный тип
}