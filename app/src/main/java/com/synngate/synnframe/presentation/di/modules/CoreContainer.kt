package com.synngate.synnframe.presentation.di.modules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.service.ClipboardServiceImpl
import com.synngate.synnframe.data.service.DeviceInfoServiceImpl
import com.synngate.synnframe.data.service.FileServiceImpl
import com.synngate.synnframe.data.service.SoundServiceImpl
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
import com.synngate.synnframe.util.resources.ResourceProviderImpl
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
        ResourceProviderImpl(appContainer.applicationContext)
    }

    override val loggingService: LoggingService by lazy {
        Timber.d("Creating LoggingService")
        // Для создания LoggingService нужен LogRepository,
        // но он находится в Data слое, поэтому создаем заглушку
        // Позже, в DataContainer, мы настроим LoggingService правильно
        object : LoggingService {
            override suspend fun logInfo(message: String): Long = 0L

            override suspend fun logWarning(message: String): Long = 0L

            override suspend fun logError(message: String): Long = 0L
        }
    }

    override val notificationChannelManager: NotificationChannelManager by lazy {
        Timber.d("Creating NotificationChannelManager")
        NotificationChannelManager(appContainer.applicationContext)
    }

    override val deviceInfoService: DeviceInfoService by lazy {
        Timber.d("Creating DeviceInfoService")
        DeviceInfoServiceImpl(appContainer.applicationContext)
    }

    override val fileService: FileService by lazy {
        Timber.d("Creating FileService")
        FileServiceImpl(appContainer.applicationContext)
    }

    override val soundService: SoundService by lazy {
        Timber.d("Creating SoundService")
        SoundServiceImpl(appContainer.applicationContext)
    }

    override val clipboardService: ClipboardService by lazy {
        Timber.d("Creating ClipboardService")
        ClipboardServiceImpl(appContainer.applicationContext)
    }

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