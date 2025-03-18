package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import timber.log.Timber

/**
 * Класс для хранения и очистки ViewModels
 */
class ViewModelStore : Clearable {
    private val viewModels = mutableMapOf<String, ViewModel>()

    /**
     * Получение ViewModel по ключу
     * @param key Ключ для идентификации ViewModel
     * @param factory Фабрика для создания ViewModel, если она не существует
     */
    fun <T : ViewModel> getOrCreate(key: String, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return viewModels.getOrPut(key) {
            Timber.d("Creating ViewModel for key: $key")
            factory()
        } as T
    }

    /**
     * Очистка ViewModel по ключу
     */
    fun clear(key: String) {
        viewModels.remove(key)?.also {
            if (it is Clearable) {
                Timber.d("Clearing ViewModel resources for key: $key")
                it.clear()
            }
        }
    }

    /**
     * Очистка всех ViewModels
     */
    override fun clear() {
        Timber.d("Clearing all ViewModels in store")
        viewModels.forEach { (key, viewModel) ->
            if (viewModel is Clearable) {
                Timber.d("Clearing ViewModel resources for key: $key")
                viewModel.clear()
            }
        }
        viewModels.clear()
    }
}

/**
 * Базовый класс для ViewModel с поддержкой очистки ресурсов
 */
abstract class ClearableViewModel : ViewModel(), Clearable {
    private val clearables = mutableListOf<Clearable>()

    /**
     * Добавление ресурса для последующей очистки
     */
    fun addClearable(clearable: Clearable) {
        clearables.add(clearable)
    }

    /**
     * Очистка всех добавленных ресурсов
     */
    override fun clear() {
        clearables.forEach { it.clear() }
        clearables.clear()
    }

    /**
     * Автоматическая очистка ресурсов при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        clear()
    }
}