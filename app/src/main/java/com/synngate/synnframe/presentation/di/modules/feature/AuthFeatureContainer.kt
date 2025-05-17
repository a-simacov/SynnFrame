package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

/**
 * Контейнер для функциональности авторизации.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с авторизацией.
 */
class AuthFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Auth"

    /**
     * Создание ViewModel для экрана входа в систему
     */
    fun createLoginViewModel(): LoginViewModel {
        return getViewModel("LoginViewModel") {
            LoginViewModel(
                userUseCases = domainContainer.userUseCases,
                serverUseCases = domainContainer.serverUseCases,
                deviceInfoService = coreContainer.deviceInfoService,
                ioDispatcher = Dispatchers.IO
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Auth feature initialized")
    }
}