package com.synngate.synnframe.presentation.di

import androidx.lifecycle.ViewModel
import timber.log.Timber

class ViewModelStore : Clearable {
    private val viewModels = mutableMapOf<String, ViewModel>()

    fun <T : ViewModel> getOrCreate(key: String, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return viewModels.getOrPut(key) {
            Timber.d("Creating ViewModel for key: $key")
            factory()
        } as T
    }

    fun clear(key: String) {
        viewModels.remove(key)?.also {
            if (it is Clearable) {
                Timber.d("Clearing ViewModel resources for key: $key")
                it.clear()
            }
        }
    }

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

abstract class ClearableViewModel : ViewModel(), Clearable {
    private val clearables = mutableListOf<Clearable>()

    fun addClearable(clearable: Clearable) {
        clearables.add(clearable)
    }

    override fun clear() {
        clearables.forEach { it.clear() }
        clearables.clear()
    }

    override fun onCleared() {
        super.onCleared()
        clear()
    }
}