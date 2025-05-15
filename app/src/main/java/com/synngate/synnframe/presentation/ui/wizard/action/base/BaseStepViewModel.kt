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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        initStateFromContext()

        context.lastScannedBarcode?.let { barcode ->
            processBarcode(barcode)
        }

        isInitializing = false
    }

    protected open fun initStateFromContext() {
        try {
            if (context.hasStepResult) {
                val result = context.getCurrentStepResult()
                if (result != null) {
                    try {
                        if (isValidType(result)) {
                            @Suppress("UNCHECKED_CAST")
                            val typedResult = result as T

                            _state.update {
                                it.copy(data = typedResult)
                            }

                            onResultLoadedFromContext(typedResult)
                        }
                    } catch (e: ClassCastException) {
                        handleException(e, "приведения результата к ожидаемому типу")
                    }
                }
            }

            if (context.validationError != null) {
                _state.update {
                    it.copy(error = context.validationError)
                }
            }
        } catch (e: Exception) {
            handleException(e, "инициализации состояния из контекста")
        }
    }

    protected open fun onResultLoadedFromContext(result: T) {
        // Базовая реализация пуста
    }

    protected abstract fun isValidType(result: Any): Boolean

    abstract fun processBarcode(barcode: String)

    protected fun handleException(e: Exception, operation: String) {
        setError("Ошибка при $operation: ${e.message}")
        setLoading(false)

        resetAutoTransition()
    }

    protected fun executeWithErrorHandling(
        operation: String,
        showLoading: Boolean = true,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (showLoading) setLoading(true)
                setError(null)

                block()

                if (showLoading) setLoading(false)
            } catch (e: Exception) {
                handleException(e, operation)
            }
        }
    }

    open fun validateData(data: T?): Boolean {
        if (data == null) return false

        if (!validateBasicRules(data)) {
            resetAutoTransition()
            return false
        }

        val validationRules = step.validationRules

        if (validationRules.rules.isEmpty()) return true

        val hasApiValidation = validationRules.rules.any {
            it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
        }

        if (hasApiValidation) {
            executeWithErrorHandling("валидации данных") {
                val validationContext = createValidationContext()

                when (val result = validationService.validate(validationRules, data, validationContext)) {
                    is ValidationResult.Success -> {
                        setData(data)

                        delay(100)

                        context.onComplete(data)
                    }
                    is ValidationResult.Error -> {
                        setError(result.message)

                        resetAutoTransition()
                    }
                }
            }
            return true
        }

        return true
    }

    protected open fun createValidationContext(): Map<String, Any> {
        return context.results
    }

    protected open fun validateBasicRules(data: T?): Boolean {
        return true
    }

    fun completeStep(result: T) {
        executeWithErrorHandling("завершения шага") {
            if (validateData(result)) {
                val hasApiValidation = step.validationRules.rules.any {
                    it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
                }

                if (!hasApiValidation) {
                    setData(result)
                    delay(50)
                    context.onComplete(result)
                }
            } else {
                setError("Некорректные данные для завершения шага")
            }
        }
    }

    protected fun setData(data: T?) {
        _state.update { it.copy(data = data, error = null) }
    }

    protected fun setError(message: String?) {
        _state.update { it.copy(error = message) }

        if (message != null) {
            resetAutoTransition()
        }
    }

    protected fun setLoading(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    protected fun updateAdditionalData(key: String, value: Any) {
        _state.update {
            val newAdditionalData = it.additionalData.toMutableMap()
            newAdditionalData[key] = value
            it.copy(additionalData = newAdditionalData)
        }

        resetAutoTransition()
    }

    protected fun resetAutoTransition() {
        if (autoTransitionActivated) {
            autoTransitionActivated = false
        }
    }

    fun goBack() {
        context.onBack()
    }

    protected fun handleFieldUpdate(fieldName: String, value: T, forceAutoTransition: Boolean = false) {
        setData(value)

        if (isInitializing || autoTransitionActivated) {
            return
        }

        if (!forceAutoTransition && !shouldAutoTransition(fieldName)) {
            return
        }

        autoTransitionActivated = true

        if (requiresConfirmationForAutoTransition(fieldName)) {
            showConfirmationDialog(value)
        } else {
            completeStep(value)
        }
    }

    protected fun shouldAutoTransition(fieldName: String): Boolean {
        if (stepFactory !is AutoCompleteCapableFactory) {
            return false
        }

        return (stepFactory as AutoCompleteCapableFactory).let { factory ->
            factory.isAutoCompleteEnabled(step) &&
                    factory.getAutoCompleteFieldName(step) == fieldName
        }
    }

    protected fun requiresConfirmationForAutoTransition(fieldName: String): Boolean {
        if (stepFactory !is AutoCompleteCapableFactory) {
            return false
        }

        return (stepFactory as AutoCompleteCapableFactory).requiresConfirmation(step, fieldName)
    }

    protected open fun showConfirmationDialog(value: T) { }

    fun validateAndCompleteIfValid(data: T) {
        executeWithErrorHandling("валидации данных") {
            if (validateData(data)) {
                val hasApiValidation = step.validationRules.rules.any {
                    it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
                }

                if (!hasApiValidation) {
                    context.onComplete(data)
                }
            } else {
                setError("Некорректные данные для завершения шага")
            }
        }
    }
}