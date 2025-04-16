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
        Timber.d("Disposing container: ${this::class.java.simpleName}")

        // Освобождаем дочерние контейнеры
        childContainers.forEach { it.dispose() }
        childContainers.clear()

        // Освобождаем ViewModel, поддерживающие специальный интерфейс
        viewModels.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }
        viewModels.clear()
    }
}