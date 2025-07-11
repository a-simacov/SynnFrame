package com.synngate.synnframe.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.presentation.di.Disposable
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
 * Базовый класс для всех ViewModel с поддержкой SavedStateHandle
 * @param S Тип состояния экрана
 * @param E Тип событий экрана
 * @param savedStateHandle SavedStateHandle для сохранения состояния при изменении конфигурации
 * @param initialState Начальное состояние
 * @param ioDispatcher Диспетчер для операций ввода-вывода
 */
abstract class BaseViewModelWithSavedState<S, E>(
    protected val savedStateHandle: SavedStateHandle,
    initialState: S,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel(), Disposable {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    // События (одноразовые, например навигация или снэкбары)
    private val _events = MutableSharedFlow<E>()
    val events: SharedFlow<E> = _events.asSharedFlow()

    // Обработчик исключений в корутинах
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Unhandled exception in ViewModel: ${this::class.java.simpleName}")
    }

    protected fun updateState(update: (S) -> S) {
        _uiState.value = update(_uiState.value)
    }

    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    protected fun handleStateEvent(event: StateEventHandler<S>) {
        updateState { currentState -> event.handle(currentState) }
    }

    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(ioDispatcher + exceptionHandler) {
            block()
        }
    }

    protected fun launchMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            block()
        }
    }

    /**
     * Сохраняет значение в SavedStateHandle
     */
    protected fun <T> setSavedState(key: String, value: T) {
        savedStateHandle[key] = value
    }

    /**
     * Получает значение из SavedStateHandle
     */
    protected fun <T> getSavedState(key: String): T? {
        return savedStateHandle[key]
    }

    /**
     * Получает значение из SavedStateHandle с значением по умолчанию
     */
    protected fun <T> getSavedState(key: String, defaultValue: T): T {
        return savedStateHandle[key] ?: defaultValue
    }

    override fun dispose() {
        Timber.d("Disposing ViewModel: ${this::class.java.simpleName}")
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}