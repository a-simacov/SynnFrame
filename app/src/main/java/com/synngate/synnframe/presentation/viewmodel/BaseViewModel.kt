package com.synngate.synnframe.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.presentation.di.Clearable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Базовый класс для всех ViewModel
 */
abstract class BaseViewModel<S, E>(
    initialState: S,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel(), Clearable {

    // UI состояние
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    // События (одноразовые, например навигация или снэкбары)
    private val _events = MutableSharedFlow<E>()
    val events: SharedFlow<E> = _events.asSharedFlow()

    // Обработчик исключений в корутинах
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Unhandled exception in ViewModel")
        // Здесь можно добавить логику обработки ошибок по умолчанию
    }

    /**
     * Обновляет UI состояние
     */
    protected fun updateState(update: (S) -> S) {
        _uiState.value = update(_uiState.value)
    }

    /**
     * Отправляет событие
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    /**
     * Запускает корутину в viewModelScope на IO диспетчере
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(ioDispatcher + exceptionHandler) {
            block()
        }
    }

    /**
     * Запускает корутину в viewModelScope на Main диспетчере
     */
    protected fun launchMain(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            block()
        }
    }

    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    override fun clear() {
        // Переопределяется в подклассах при необходимости
    }

    /**
     * Вызывается при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        clear()
    }
}