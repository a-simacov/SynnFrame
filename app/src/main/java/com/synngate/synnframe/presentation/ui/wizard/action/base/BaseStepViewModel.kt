// Заменяет com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Улучшенный базовый класс ViewModel для шагов визарда.
 * Добавляет встроенную поддержку автоперехода и надежную передачу данных.
 *
 * @param T тип данных, используемый на данном шаге
 * @property step текущий шаг действия
 * @property action запланированное действие
 * @property context контекст выполнения действия
 * @property validationService сервис для валидации данных
 * @property stepFactory фабрика компонентов шага (используется для автоперехода)
 */
abstract class BaseStepViewModel<T>(
    protected val step: ActionStep,
    protected val action: PlannedAction,
    protected val context: ActionContext,
    protected val validationService: ValidationService,
    protected val stepFactory: ActionStepFactory? = null
) : ViewModel() {

    private val _state = MutableStateFlow(StepViewState<T>())
    val state: StateFlow<StepViewState<T>> = _state.asStateFlow()

    // Флаг для предотвращения повторного автоперехода при инициализации
    private var isInitializing = true

    // Флаг, указывающий, был ли активирован автопереход
    private var autoTransitionActivated = false

    init {
        Timber.d("Инициализация ${this::class.java.simpleName} для шага ${step.id}")

        // Инициализируем состояние из контекста
        initStateFromContext()

        // Обрабатываем штрих-код из контекста, если он есть
        context.lastScannedBarcode?.let { barcode ->
            processBarcode(barcode)
        }

        // Сбрасываем флаг инициализации
        isInitializing = false
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
                            val typedResult = result as T

                            // Устанавливаем данные в состояние
                            _state.update {
                                it.copy(data = typedResult)
                            }

                            // Уведомляем подклассы о загрузке данных из контекста
                            onResultLoadedFromContext(typedResult)
                        } else {
                            Timber.w("Несовместимый тип для шага ${context.stepId}: " +
                                    "найден ${result::class.simpleName}")
                        }
                    } catch (e: ClassCastException) {
                        Timber.e(e, "Ошибка приведения результата к ожидаемому типу")
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
            Timber.e(e, "Ошибка инициализации состояния из контекста")
            setError("Ошибка инициализации состояния: ${e.message}")
        }
    }

    /**
     * Вызывается при загрузке результата из контекста.
     * Подклассы могут переопределить этот метод для обработки загруженных данных.
     */
    protected open fun onResultLoadedFromContext(result: T) {
        // Базовая реализация пуста
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

        // Проверяем базовые правила
        if (!validateBasicRules(data)) {
            return false
        }

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

        return true
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
                    // Валидация прошла успешно

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

    /**
     * Обрабатывает обновление поля и выполняет автопереход при необходимости
     * @param fieldName Название поля
     * @param value Значение поля
     * @param forceAutoTransition Принудительно выполнить автопереход, игнорируя проверки
     */
    protected fun handleFieldUpdate(fieldName: String, value: T, forceAutoTransition: Boolean = false) {
        // Обновляем данные
        setData(value)

        // Если мы на этапе инициализации или автопереход уже активирован,
        // не запускаем его снова
        if (isInitializing || autoTransitionActivated) {
            return
        }

        // Если автопереход принудительно запрещен, не делаем его
        if (!forceAutoTransition && !shouldAutoTransition(fieldName)) {
            return
        }

        // Отмечаем, что автопереход был активирован
        autoTransitionActivated = true

        // Проверяем, требуется ли подтверждение перед автопереходом
        if (requiresConfirmationForAutoTransition(fieldName)) {
            showConfirmationDialog(value)
        } else {
            // Автоматически завершаем шаг
            completeStep(value)
        }
    }

    /**
     * Проверяет, нужен ли автопереход для текущего поля
     */
    protected fun shouldAutoTransition(fieldName: String): Boolean {
        if (stepFactory !is AutoCompleteCapableFactory) {
            return false
        }

        return (stepFactory as AutoCompleteCapableFactory).let { factory ->
            factory.isAutoCompleteEnabled(step) &&
                    factory.getAutoCompleteFieldName(step) == fieldName
        }
    }

    /**
     * Проверяет, требуется ли подтверждение перед автопереходом
     */
    protected fun requiresConfirmationForAutoTransition(fieldName: String): Boolean {
        if (stepFactory !is AutoCompleteCapableFactory) {
            return false
        }

        return (stepFactory as AutoCompleteCapableFactory).requiresConfirmation(step, fieldName)
    }

    /**
     * Показывает диалог подтверждения перед автопереходом
     * Подклассы должны переопределить этот метод если поддерживают автопереход
     */
    protected open fun showConfirmationDialog(value: T) {
        // По умолчанию пустая реализация
        // Подклассы должны переопределить, если требуется подтверждение
        Timber.w("showConfirmationDialog не реализован в ${this::class.java.simpleName}")
    }
}