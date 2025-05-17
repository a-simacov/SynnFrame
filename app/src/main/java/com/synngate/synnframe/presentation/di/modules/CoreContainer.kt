package com.synngate.synnframe.presentation.di.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.DeviceInfoService
import com.synngate.synnframe.domain.service.FileService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.CoreAPI
import com.synngate.synnframe.presentation.di.modules.api.ModuleAPI
import com.synngate.synnframe.presentation.service.notification.NotificationChannelManager
import com.synngate.synnframe.util.resources.ResourceProvider
import timber.log.Timber

/**
 * Модульный контейнер для базовых компонентов приложения (настройки, ресурсы, логирование и т.д.)
 *
 * @param appContainer Основной контейнер приложения
 */
class CoreContainer(appContainer: AppContainer) : ModuleContainer(appContainer), CoreAPI, ModuleAPI {

    override val moduleName: String = "Core"

    // DataStore для хранения настроек приложения
    override val preferencesDataStore: DataStore<Preferences> by lazy {
        appContainer.applicationContext.dataStore
    }

    override val appSettingsDataStore: AppSettingsDataStore by lazy {
        Timber.d("Creating AppSettingsDataStore")
        AppSettingsDataStore(preferencesDataStore)
    }

    override val resourceProvider: ResourceProvider by lazy {
        Timber.d("Creating ResourceProvider")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val loggingService: LoggingService by lazy {
        Timber.d("Creating LoggingService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val notificationChannelManager: NotificationChannelManager by lazy {
        Timber.d("Creating NotificationChannelManager")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val deviceInfoService: DeviceInfoService by lazy {
        Timber.d("Creating DeviceInfoService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val fileService: FileService by lazy {
        Timber.d("Creating FileService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val soundService: SoundService by lazy {
        Timber.d("Creating SoundService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    override val clipboardService: ClipboardService by lazy {
        Timber.d("Creating ClipboardService")
        // Будет реализовано на следующем этапе
        throw NotImplementedError("Not implemented in this phase")
    }

    // Этот метод должен быть расширен в следующих фазах
    override fun initialize() {
        super.initialize()
        Timber.d("Core module initialized")
    }

    override fun cleanup() {
        Timber.d("Cleaning up Core module")
    }
}

// Для поддержки DataStore в CoreContainer
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")