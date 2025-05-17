package com.synngate.synnframe.presentation.di.modules

import androidx.lifecycle.ViewModel
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.api.FeatureAPI
import timber.log.Timber

/**
 * Базовый класс для всех функциональных контейнеров.
 * Содержит общую логику для создания ViewModel-ей и управления ресурсами.
 *
 * @param appContainer Основной контейнер приложения
 * @param coreContainer Контейнер базовых компонентов
 * @param domainContainer Контейнер бизнес-логики
 */
abstract class FeatureContainer(
    appContainer: AppContainer,
    protected val coreContainer: CoreContainer,
    protected val domainContainer: DomainContainer
) : ModuleContainer(appContainer), FeatureAPI {

    // Сохраняем созданные ViewModel-и
    private val viewModels = mutableMapOf<String, ViewModel>()

    /**
     * Получение или создание ViewModel
     *
     * @param key Уникальный ключ для ViewModel
     * @param factory Фабрика для создания ViewModel
     * @return Созданная или существующая ViewModel
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> getViewModel(key: String, factory: () -> T): T {
        return viewModels.getOrPut(key) {
            Timber.d("Creating ViewModel: $key for feature: $moduleName")
            factory()
        } as T
    }

    /**
     * Освобождение ресурсов контейнера
     */
    override fun dispose() {
        // Освобождаем ViewModel-и, поддерживающие интерфейс Disposable
        viewModels.values.forEach { viewModel ->
            if (viewModel is com.synngate.synnframe.presentation.di.Disposable) {
                viewModel.dispose()
            }
        }
        viewModels.clear()

        super.dispose()
    }

    /**
     * Очистка ресурсов модуля
     */
    override fun cleanup() {
        Timber.d("Cleaning up feature: $moduleName")
        dispose()
    }
}