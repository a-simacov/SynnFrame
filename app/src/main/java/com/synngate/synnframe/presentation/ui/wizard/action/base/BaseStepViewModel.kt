package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.AutoFillManager
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.service.ValidationResult
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseStepViewModel<T: Any>(
    protected val step: ActionStep,
    protected val action: PlannedAction,
    protected val context: ActionContext,
    protected val validationService: ValidationService,
    private val taskContextManager: TaskContextManager?,
    protected val stepFactory: ActionStepFactory? = null
) : ViewModel(), Disposable {

    private val _state = MutableStateFlow(StepViewState<T>())
    val state: StateFlow<StepViewState<T>> = _state.asStateFlow()

    private var isInitializing = true
    private var autoTransitionActivated = false
    private var autoFilledApplied = false
    private var wasAutoFilledInThisSession = false
    private var userModifiedData = false
    private var isNavigatingBackToThisStep = false
    private val activeJobs = mutableListOf<Job>()
    private val objectsMarkedForSaving = mutableMapOf<ActionObjectType, Any>()

    // Создаем AutoFillManager для работы с автозаполнением
    private val autoFillManager = taskContextManager?.let { AutoFillManager(it) }

    init {
        // Проверяем, вернулись ли мы на этот шаг
        isNavigatingBackToThisStep = checkIfNavigatingBack()

        initStateFromContext()

        context.lastScannedBarcode?.let { barcode ->
            processBarcode(barcode)
        }

        isInitializing = false
        checkAndApplyAutoFill()
    }

    // Проверяем, произошел ли возврат на этот шаг
    private fun checkIfNavigatingBack(): Boolean {
        return try {
            val wizardState = context.results["wizardState"] as? Map<*, *>
            wizardState?.get("isNavigatingBack") as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
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
        Timber.e(e, "Ошибка в ViewModel ${this.javaClass.simpleName}: $operation")
    }

    protected fun executeWithErrorHandling(
        operation: String,
        showLoading: Boolean = true,
        block: suspend () -> Unit
    ) {
        val job = viewModelScope.launch {
            try {
                if (showLoading) setLoading(true)
                setError(null)

                block()

                if (showLoading) setLoading(false)
            } catch (e: Exception) {
                handleException(e, operation)
            }
        }

        synchronized(activeJobs) {
            activeJobs.add(job)
            job.invokeOnCompletion {
                synchronized(activeJobs) {
                    activeJobs.remove(job)
                }
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

                markObjectsForSaving(result)

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

    private fun checkAndApplyAutoFill() {
        if (autoFilledApplied || autoFillManager == null) return

        // Используем AutoFillManager для проверки возможности автозаполнения
        if (!autoFillManager.canAutoFillStep(action, step, stepFactory)) {
            return
        }

        val autoFillData = autoFillManager.getAutoFillData<Any>(step)
        if (autoFillData != null) {
            executeWithErrorHandling("применения автозаполнения", showLoading = false) {
                val success = applyAutoFill(autoFillData)
                if (success) {
                    wasAutoFilledInThisSession = true
                    Timber.d("Автозаполнение применено, isNavigatingBack = $isNavigatingBackToThisStep")
                }
            }
        }
    }

    protected open fun applyAutoFill(data: Any): Boolean {
        try {
            if (isValidType(data)) {
                @Suppress("UNCHECKED_CAST")
                val typedData = data as T

                setData(typedData)
                autoFilledApplied = true

                // Сбрасываем флаг пользовательских изменений при успешном автозаполнении
                userModifiedData = false
                Timber.d("Автозаполнение выполнено, флаг userModifiedData сброшен")

                updateAdditionalData("autoFilled", true)

                handleAutoFillCompletion(typedData)

                return true
            }
            return false
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при применении автозаполнения: ${e.message}")
            return false
        }
    }

    private fun handleAutoFillCompletion(data: T) {
        if (stepFactory !is AutoCompleteCapableFactory) return

        val shouldAutoComplete = (stepFactory as AutoCompleteCapableFactory).shouldAutoComplete(step)

        // Если мы вернулись на этот шаг назад, не выполняем автозавершение
        if (shouldAutoComplete && !isNavigatingBackToThisStep) {
            Timber.d("Выполняем автозавершение шага: ${step.id}")
            viewModelScope.launch {
                delay(300)

                if (validateData(data)) {
                    completeStep(data)
                }
            }
        } else {
            Timber.d("Автозавершение пропущено: shouldAutoComplete=$shouldAutoComplete, isNavigatingBack=$isNavigatingBackToThisStep")
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

        // Помечаем как пользовательские изменения только определенные ключи
        if (key != "autoFilled" &&
            !key.startsWith("markedForSaving_") &&
            !key.startsWith("savableObject_") &&
            !key.startsWith("showCameraScannerDialog") &&
            !key.startsWith("showProductSelectionDialog") &&
            !key.startsWith("filteredBins") &&
            !key.startsWith("filteredPallets")) {

            // Устанавливаем флаг пользовательских изменений только если уже было автозаполнение
            if (wasAutoFilledInThisSession) {
                userModifiedData = true
                Timber.d("Пользователь изменил автозаполненные данные: ключ = $key")
            } else {
                Timber.d("Пользовательский ввод для поиска: ключ = $key")
            }
        }

        // Вызываем resetAutoTransition только если это не обновление флага autoFilled
        if (key != "autoFilled") {
            resetAutoTransition()
        }
    }

    protected fun resetAutoTransition() {
        if (autoTransitionActivated) {
            autoTransitionActivated = false
        }

        // Сбрасываем флаг автозаполнения только если пользователь действительно изменил данные
        if (userModifiedData && wasAutoFilledInThisSession) {
            wasAutoFilledInThisSession = false
            Timber.d("Сброс флага автозаполнения из-за пользовательских изменений")
            // Напрямую обновляем состояние без вызова updateAdditionalData
            _state.update {
                val newAdditionalData = it.additionalData.toMutableMap()
                newAdditionalData["autoFilled"] = false
                it.copy(additionalData = newAdditionalData)
            }
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

        // Если данные были изменены пользователем после автозаполнения, не выполняем автопереход
        if (userModifiedData && !forceAutoTransition) {
            Timber.d("Данные изменены пользователем, автопереход отключен")
            return
        }

        // Если мы вернулись на этот шаг, не выполняем автопереход (только если не принудительный)
        if (isNavigatingBackToThisStep && !forceAutoTransition) {
            Timber.d("Возврат на шаг, автопереход отключен")
            return
        }

        autoTransitionActivated = true
        Timber.d("Выполняем автопереход для поля: $fieldName, wasAutoFilled: $wasAutoFilledInThisSession, userModified: $userModifiedData")

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

    fun markObjectForSaving(objectType: ActionObjectType, data: Any) {
        objectsMarkedForSaving[objectType] = data
        updateAdditionalData("markedForSaving_$objectType", true)
        Timber.d("Объект типа $objectType помечен для сохранения, userModifiedData = $userModifiedData")
    }

    private fun markObjectsForSaving(result: Any) {
        if (objectsMarkedForSaving.isEmpty()) {
            autoMarkObjectForSaving(result)
        }

        val taskType = taskContextManager?.lastTaskTypeX?.value
        val savableObjectTypes = taskType?.savableObjectTypes ?: emptyList()

        for ((type, data) in objectsMarkedForSaving) {
            if (type in savableObjectTypes) {
                updateAdditionalData("savableObject_$type", data)
                context.onUpdate(mapOf("savableObject_$type" to data))

                Timber.d("Объект типа $type помечен для сохранения и добавлен в additionalData")
            }
        }
    }

    private fun autoMarkObjectForSaving(result: Any) {
        if (taskContextManager == null) return
        val taskType = taskContextManager.lastTaskTypeX.value ?: return

        val objectType = step.objectType
        if (objectType in taskType.savableObjectTypes) {
            objectsMarkedForSaving[objectType] = result
        }
    }

    override fun dispose() {
        Timber.d("Disposing ViewModel ${this.javaClass.simpleName}")

        synchronized(activeJobs) {
            activeJobs.forEach { job ->
                if (job.isActive) {
                    job.cancel()
                }
            }
            activeJobs.clear()
        }

        objectsMarkedForSaving.clear()
        onDispose()
    }

    protected open fun onDispose() {
        // Пустая реализация для переопределения в наследниках
    }
}