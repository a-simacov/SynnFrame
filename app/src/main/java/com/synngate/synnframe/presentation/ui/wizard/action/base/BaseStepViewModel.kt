package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Базовый класс ViewModel для шагов визарда.
 *
 * @param T тип данных, используемый на данном шаге
 * @property step текущий шаг действия
 * @property action запланированное действие
 * @property context контекст выполнения действия
 */
abstract class BaseStepViewModel<T>(
    protected val step: ActionStep,
    protected val action: PlannedAction,
    protected val context: ActionContext
) : ViewModel() {

    private val _state = MutableStateFlow(StepViewState<T>())
    val state: StateFlow<StepViewState<T>> = _state.asStateFlow()

    init {
        // Инициализируем состояние из контекста
        initStateFromContext()

        // Обрабатываем штрих-код из контекста, если он есть
        context.lastScannedBarcode?.let { barcode ->
            processBarcode(barcode)
        }
    }

    /**
     * Инициализирует состояние из контекста.
     * Подклассы могут переопределить этот метод для своей логики инициализации.
     */
    protected open fun initStateFromContext() {
        // Проверяем, есть ли уже результат в контексте для текущего шага
        if (context.hasStepResult) {
            val result = context.getCurrentStepResult()
            @Suppress("UNCHECKED_CAST")
            if (result != null && isValidType(result)) {
                _state.update {
                    it.copy(data = result as T)
                }
            }
        }

        // Проверяем, есть ли ошибка в контексте для текущего шага
        if (context.validationError != null) {
            _state.update {
                it.copy(error = context.validationError)
            }
        }
    }

    /**
     * Проверяет, является ли результат правильного типа для данного ViewModel.
     * Подклассы должны переопределить этот метод для проверки типа.
     */
    protected abstract fun isValidType(result: Any): Boolean

    /**
     * Обрабатывает штрих-код.
     * Подклассы должны переопределить этот метод для обработки штрих-кода.
     */
    abstract fun processBarcode(barcode: String)

    /**
     * Валидирует данные перед завершением шага.
     * @return true, если данные валидны, иначе false
     */
    abstract fun validateData(data: T?): Boolean

    /**
     * Завершает шаг с указанным результатом, если данные валидны.
     */
    fun completeStep(result: T) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                if (validateData(result)) {
                    context.onComplete(result)
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Некорректные данные для завершения шага"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при завершении шага: ${step.id}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Устанавливает новые данные в состояние.
     */
    protected fun setData(data: T?) {
        _state.update { it.copy(data = data, error = null) }
    }

    /**
     * Устанавливает ошибку в состояние.
     */
    protected fun setError(message: String?) {
        _state.update { it.copy(error = message) }
    }

    /**
     * Устанавливает флаг загрузки в состояние.
     */
    protected fun setLoading(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    /**
     * Обновляет дополнительные данные в состоянии.
     */
    protected fun updateAdditionalData(key: String, value: Any) {
        _state.update {
            val newAdditionalData = it.additionalData.toMutableMap()
            newAdditionalData[key] = value
            it.copy(additionalData = newAdditionalData)
        }
    }

    /**
     * Возвращает к предыдущему шагу.
     */
    fun goBack() {
        context.onBack()
    }
}