package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import com.synngate.synnframe.presentation.di.modules.feature.AuthFeatureContainer
import com.synngate.synnframe.presentation.ui.login.LoginViewModel
import timber.log.Timber

/**
 * Контейнер для уровня экрана.
 * Содержит фабрики для создания ViewModel-ей для конкретного экрана,
 * делегируя создание соответствующим функциональным контейнерам.
 */
class ScreenContainerNew(
    private val appContainer: AppContainer,
    private val navigationContainer: NavigationContainer,
    private val navGraphRoute: String? = null
) : DiContainer() {

    // Хранилище для локальных (не делегированных) ViewModel-ей
    private val localViewModels = mutableMapOf<String, ViewModel>()

    /**
     * Создание ViewModel для экрана входа в систему
     */
    fun createLoginViewModel(): LoginViewModel {
        val authContainer = navigationContainer.getNavigationGraphContainer<AuthFeatureContainer>("auth_graph")
        return authContainer.createLoginViewModel()
    }

    /**
     * Получение или создание локальной ViewModel
     * Используется для ViewModel-ей, которые не делегируются функциональным контейнерам
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> getOrCreateViewModel(key: String, factory: () -> T): T {
        return localViewModels.getOrPut(key) {
            Timber.d("Creating local ViewModel: $key in ScreenContainer")
            factory()
        } as T
    }

    /**
     * Заглушки для всех остальных фабрик ViewModel-ей
     * Будут реализованы в следующих этапах
     */

    // Экраны продуктов
    fun createProductListViewModel(isSelectionMode: Boolean): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createProductDetailViewModel(productId: String): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Экраны заданий
    fun createTaskXListViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createTaskXDetailViewModel(taskId: String): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createActionWizardViewModel(taskId: String, actionId: String): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Экраны логов
    fun createLogListViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createLogDetailViewModel(logId: Int): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Экраны настроек
    fun createSettingsViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createSyncHistoryViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Экраны серверов
    fun createServerListViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    fun createServerDetailViewModel(serverId: Int?): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Главное меню
    fun createMainMenuViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    // Экраны динамического меню
    fun createDynamicMenuViewModel(): ViewModel {
        throw NotImplementedError("Not implemented in this phase")
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        Timber.d("Disposing ScreenContainer")

        // Освобождаем локальные ViewModel-и
        localViewModels.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }
        localViewModels.clear()

        super.dispose()
    }
}