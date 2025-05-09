package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
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
 * @property validationService сервис для валидации данных
 */
abstract class BaseStepViewModel<T>(
    protected val step: ActionStep,
    protected val action: PlannedAction,
    protected val context: ActionContext,
    protected val validationService: ValidationService
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
        try {
            // Проверяем, есть ли уже результат в контексте для текущего шага
            if (context.hasStepResult) {
                val result = context.getCurrentStepResult()
                if (result != null) {
                    try {
                        if (isValidType(result)) {
                            @Suppress("UNCHECKED_CAST")
                            _state.update {
                                it.copy(data = result as T)
                            }
                        } else {
                            Timber.w("Incompatible type for step ${context.stepId}: found ${result::class.simpleName}")
                        }
                    } catch (e: ClassCastException) {
                        Timber.e(e, "Error casting result to expected type")
                        setError("Ошибка при загрузке данных: несовместимый тип данных")
                    }
                }
            }

            // Проверяем, есть ли ошибка в контексте для текущего шага
            if (context.validationError != null) {
                _state.update {
                    it.copy(error = context.validationError)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing state from context")
            setError("Ошибка инициализации состояния: ${e.message}")
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
     * Использует ValidationService для проверки данных по правилам шага.
     *
     * @param data данные для валидации
     * @return true, если данные валидны, иначе false
     */
    open fun validateData(data: T?): Boolean {
        if (data == null) return false

        // Получаем правила валидации из шага
        val validationRules = step.validationRules

        // Если правила отсутствуют, считаем данные валидными
        if (validationRules.rules.isEmpty()) return true

        // Проверяем, есть ли API-правила валидации
        val hasApiValidation = validationRules.rules.any {
            it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
        }

        // Если есть API-правила, запускаем асинхронную валидацию
        if (hasApiValidation) {
            viewModelScope.launch {
                setLoading(true)

                // Создаем контекст для валидации
                val validationContext = createValidationContext()

                // Вызываем валидацию
                when (val result = validationService.validate(validationRules, data, validationContext)) {
                    is ValidationResult.Success -> {
                        // Продолжаем действие после успешной валидации
                        context.onComplete(data)
                    }
                    is ValidationResult.Error -> {
                        // Отображаем ошибку
                        setError(result.message)
                    }
                }

                setLoading(false)
            }
            // Возвращаем true, чтобы показать, что валидация запущена
            // Реальный результат будет обработан асинхронно
            return true
        }

        // Выполняем синхронную базовую валидацию
        return validateBasicRules(data)
    }

    /**
     * Создает контекст для валидации
     */
    protected open fun createValidationContext(): Map<String, Any> {
        // Базовая реализация возвращает основной контекст из ActionContext
        return context.results
    }

    /**
     * Проверяет базовые правила валидации
     */
    protected open fun validateBasicRules(data: T?): Boolean {
        // Базовая логика валидации, которая не требует API-запросов
        // Подклассы могут расширить эту логику
        return true
    }

    /**
     * Завершает шаг с указанным результатом, если данные валидны.
     */
    fun completeStep(result: T) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                if (validateData(result)) {
                    // Обратите внимание, что ValidationService может асинхронно
                    // вызвать context.onComplete, поэтому здесь мы не вызываем его повторно,
                    // если метод validateData вернул true из-за API-валидации

                    // Для не-API валидации, вызываем onComplete напрямую
                    val hasApiValidation = step.validationRules.rules.any {
                        it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
                    }

                    if (!hasApiValidation) {
                        context.onComplete(result)
                    }
                    // Иначе context.onComplete будет вызван асинхронно после API-валидации
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