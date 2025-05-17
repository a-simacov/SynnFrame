package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerListViewModel
import com.synngate.synnframe.presentation.ui.settings.SettingsViewModel
import com.synngate.synnframe.presentation.ui.sync.SyncHistoryViewModel
import timber.log.Timber

/**
 * Контейнер для функциональности работы с настройками.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с настройками и серверами.
 */
class SettingsFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Settings"

    /**
     * Создание ViewModel для экрана настроек
     *
     * @return ViewModel для настроек
     */
    fun createSettingsViewModel(): SettingsViewModel {
        return getViewModel("SettingsViewModel") {
            SettingsViewModel(
                settingsUseCases = domainContainer.settingsUseCases,
                serverUseCases = domainContainer.serverUseCases,
                webServerManager = domainContainer.webServerManager,
                synchronizationController = domainContainer.synchronizationController,
                updateInstaller = domainContainer.updateInstaller
            )
        }
    }

    /**
     * Создание ViewModel для экрана истории синхронизации
     *
     * @return ViewModel для истории синхронизации
     */
    fun createSyncHistoryViewModel(): SyncHistoryViewModel {
        return getViewModel("SyncHistoryViewModel") {
            SyncHistoryViewModel(
                synchronizationController = domainContainer.synchronizationController
            )
        }
    }

    /**
     * Создание ViewModel для экрана списка серверов
     *
     * @return ViewModel для списка серверов
     */
    fun createServerListViewModel(): ServerListViewModel {
        return getViewModel("ServerListViewModel") {
            ServerListViewModel(
                serverUseCases = domainContainer.serverUseCases,
                settingsUseCases = domainContainer.settingsUseCases
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей сервера
     *
     * @param serverId Идентификатор сервера (null для создания нового)
     * @return ViewModel для деталей сервера
     */
    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel {
        return getViewModel("ServerDetailViewModel_${serverId ?: "new"}") {
            ServerDetailViewModel(
                serverId = serverId,
                serverUseCases = domainContainer.serverUseCases
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Settings feature initialized")
    }
}