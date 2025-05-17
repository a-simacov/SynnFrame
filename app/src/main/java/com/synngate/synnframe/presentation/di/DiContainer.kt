package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import timber.log.Timber

interface Disposable {

    fun dispose()
}

abstract class DiContainer : Disposable {
    // Дочерние контейнеры, которые будут освобождены при освобождении этого контейнера
    private val childContainers = mutableListOf<DiContainer>()

    // Зарегистрированные экземпляры ViewModel
    private val viewModels = mutableMapOf<String, ViewModel>()

    /**
     * Получение или создание ViewModel
     * @param key Уникальный ключ для ViewModel
     * @param factory Функция-фабрика для создания новой ViewModel
     * @return Существующий или новый экземпляр ViewModel
     */
    fun <T : ViewModel> getOrCreateViewModel(key: String, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return viewModels.getOrPut(key) {
            Timber.d("Creating ViewModel: $key")
            factory()
        } as T
    }

    /**
     * Создание дочернего контейнера
     * @param factory Функция-фабрика для создания контейнера
     * @return Созданный контейнер
     */
    fun <T : DiContainer> createChildContainer(factory: () -> T): T {
        val container = factory()
        childContainers.add(container)
        return container
    }

    /**
     * Освобождение контейнера и всех его дочерних контейнеров
     */
    override fun dispose() {
        Timber.d("Disposing DiContainer: ${this::class.java.simpleName}")

        // Освобождаем дочерние контейнеры
        val childCount = childContainers.size
        var disposedCount = 0

        childContainers.forEach { container ->
            try {
                container.dispose()
                disposedCount++
            } catch (e: Exception) {
                Timber.e(e, "Error disposing child container: ${container::class.java.simpleName}")
            }
        }
        childContainers.clear()

        if (childCount > 0) {
            Timber.d("Disposed $disposedCount of $childCount child containers")
        }

        // Освобождаем ViewModel, поддерживающие интерфейс Disposable
        val viewModelCount = viewModels.size
        disposedCount = 0

        viewModels.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                try {
                    viewModel.dispose()
                    disposedCount++
                } catch (e: Exception) {
                    Timber.e(e, "Error disposing ViewModel: ${viewModel::class.java.simpleName}")
                }
            }
        }
        viewModels.clear()

        if (viewModelCount > 0) {
            Timber.d("Disposed $disposedCount of $viewModelCount ViewModels")
        }
    }
}