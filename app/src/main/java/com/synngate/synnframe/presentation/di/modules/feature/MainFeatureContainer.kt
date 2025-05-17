package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.main.MainMenuViewModel
import timber.log.Timber

/**
 * Контейнер для функциональности главного меню приложения.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с главным меню.
 */
class MainFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Main"

    /**
     * Создание ViewModel для экрана главного меню
     *
     * @return ViewModel для главного меню
     */
    fun createMainMenuViewModel(): MainMenuViewModel {
        return getViewModel("MainMenuViewModel") {
            MainMenuViewModel(
                userUseCases = domainContainer.userUseCases,
                productUseCases = domainContainer.productUseCases,
                synchronizationController = domainContainer.synchronizationController
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Main feature initialized")
    }
}