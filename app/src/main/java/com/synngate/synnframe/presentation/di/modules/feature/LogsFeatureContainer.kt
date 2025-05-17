package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel
import com.synngate.synnframe.presentation.ui.logs.LogListViewModel
import timber.log.Timber

/**
 * Контейнер для функциональности работы с логами.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с логами.
 */
class LogsFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Logs"

    /**
     * Создание ViewModel для экрана списка логов
     *
     * @return ViewModel для списка логов
     */
    fun createLogListViewModel(): LogListViewModel {
        return getViewModel("LogListViewModel") {
            LogListViewModel(
                logUseCases = domainContainer.logUseCases
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей лога
     *
     * @param logId Идентификатор лога
     * @return ViewModel для деталей лога
     */
    fun createLogDetailViewModel(logId: Int): LogDetailViewModel {
        return getViewModel("LogDetailViewModel_$logId") {
            LogDetailViewModel(
                logId = logId,
                logUseCases = domainContainer.logUseCases,
                clipboardService = coreContainer.clipboardService
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Logs feature initialized")
    }
}