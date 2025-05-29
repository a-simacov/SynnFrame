package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.ObjectSearchService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardController
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardNetworkService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardValidator
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    taskXRepository: TaskXRepository,
    validationService: ValidationService,
    private val productUseCases: ProductUseCases
) : BaseViewModel<ActionWizardState, ActionWizardEvent>(
    ActionWizardState(
        taskId = taskId,
        actionId = actionId
    )
) {

    private val handlerFactory = FieldHandlerFactory(validationService, productUseCases)

    private val stateMachine = WizardStateMachine()
    private val validator = WizardValidator(validationService, handlerFactory)
    private val controller = WizardController(stateMachine)
    private val objectSearchService = ObjectSearchService(productUseCases, validationService)
    private val networkService = WizardNetworkService(taskXRepository)

    private var isProcessingBarcode = false
    private var resetProcessingJob: Job? = null

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        updateState { it.copy(isLoading = true) }

        objectSearchService.clearCache()

        val initResult = controller.initializeWizard(taskId, actionId)
        updateState { initResult.getNewState() }

        initResult.getNewState().plannedAction?.storageProductClassifier?.let { product ->
            loadClassifierProductInfo(product.id)
        }

        // После инициализации инициализируем текущий шаг (проверяем буфер)
        initializeStep(initResult.getNewState().currentStepIndex)
    }

    /**
     * Инициализация шага: проверка и применение значений из буфера
     */
    private fun initializeStep(stepIndex: Int) {
        val state = uiState.value
        if (stepIndex < 0 || stepIndex >= state.steps.size) return

        // Проверяем, нужно ли применить значение из буфера
        launchIO {
            val applyBufferResult = controller.applyBufferValueIfNeeded(state)
            updateState { applyBufferResult.getNewState() }

            // Если объект из буфера успешно применен
            if (applyBufferResult.getNewState() != state) {
                Timber.d("Применено значение из буфера для шага $stepIndex")

                // Для заблокированных полей (режим ALWAYS) автоматически переходим к следующему шагу
                if (applyBufferResult.getNewState().isCurrentStepLockedByBuffer()) {
                    delay(300) // Небольшая задержка для UI
                    tryAutoAdvanceFromBuffer()
                }
            }
        }
    }

    private fun loadClassifierProductInfo(productId: String) {
        launchIO {
            updateState { it.copy(isLoadingProductInfo = true) }

            try {
                val product = productUseCases.getProductById(productId)

                if (product != null) {
                    updateState {
                        it.copy(
                            classifierProductInfo = product,
                            isLoadingProductInfo = false
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoadingProductInfo = false,
                            productInfoError = "Товар $productId не найден в базе данных"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoadingProductInfo = false,
                        productInfoError = "Ошибка загрузки данных о товаре: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmCurrentStep() {
        launchIO {
            val result = controller.confirmCurrentStep(uiState.value) {
                validateCurrentStep()
            }
            updateState { result.getNewState() }

            // После перехода на новый шаг инициализируем его
            initializeStep(result.getNewState().currentStepIndex)
        }
    }

    fun previousStep() {
        val result = controller.previousStep(uiState.value)
        updateState { result.getNewState() }
    }

    fun setObjectForCurrentStep(obj: Any, autoAdvance: Boolean = true) {
        val result = controller.setObjectForCurrentStep(uiState.value, obj)
        updateState { result.getNewState() }

        if (autoAdvance) {
            Timber.d("Вызываем автопереход после установки объекта: $obj")
            tryAutoAdvance()
        }
    }

    private fun tryAutoAdvance() {
        launchIO {
            val result = controller.tryAutoAdvance(uiState.value) {
                validateCurrentStep()
            }

            if (result.isSuccess()) {
                updateState { result.getNewState() }

                // После автоперехода инициализируем новый шаг
                initializeStep(result.getNewState().currentStepIndex)
            }
        }
    }

    /**
     * Автоматический переход для объектов из буфера с режимом ALWAYS
     */
    private fun tryAutoAdvanceFromBuffer() {
        launchIO {
            val result = controller.tryAutoAdvanceFromBuffer(uiState.value)

            if (result.isSuccess()) {
                updateState { result.getNewState() }

                // Рекурсивно проверяем следующий шаг на наличие значений в буфере
                initializeStep(result.getNewState().currentStepIndex)
            }
        }
    }

    private suspend fun validateCurrentStep(): Boolean {
        val isValid = validator.validateCurrentStep(uiState.value)

        if (!isValid) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Необходимо заполнить все обязательные поля"))
        }

        return isValid
    }

    fun handleBarcode(barcode: String) {
        val currentState = determineStateType(uiState.value)
        if (currentState == WizardState.SUMMARY ||
            currentState == WizardState.EXIT_DIALOG ||
            currentState == WizardState.SENDING
        ) {
            return
        }

        // Если текущий шаг заблокирован буфером, не обрабатываем сканирования
        if (uiState.value.isCurrentStepLockedByBuffer()) {
            Timber.d("Шаг заблокирован буфером (режим ALWAYS), сканирование игнорируется")
            sendEvent(ActionWizardEvent.ShowSnackbar("Поле заблокировано буфером"))
            return
        }

        // Проверяем, не обрабатывается ли уже другое сканирование
        if (isProcessingBarcode) {
            Timber.d("Игнорирование сканирования: предыдущее еще обрабатывается")
            return
        }

        resetProcessingJob?.cancel()

        isProcessingBarcode = true

        updateState { controller.setLoading(it, true).getNewState() }

        // Запускаем задачу для гарантированного сброса флага обработки через 5 секунд
        // в любом случае (это страховка на случай сбоев)
        resetProcessingJob = viewModelScope.launch {
            delay(5000)
            if (isProcessingBarcode) {
                Timber.d("Принудительный сброс флага обработки сканирования по таймауту")
                isProcessingBarcode = false
                updateState { controller.setLoading(it, false).getNewState() }
            }
        }

        viewModelScope.launch {
            try {
                val result = objectSearchService.handleBarcode(uiState.value, barcode)

                updateState { controller.setLoading(it, false).getNewState() }

                if (result.isSuccess() && result.isProcessingRequired()) {
                    val foundObject = result.getResultData()
                    if (foundObject != null) {
                        setObjectForCurrentStep(foundObject, true)
                    }
                } else if (!result.isSuccess()) {
                    val errorMessage = result.getErrorMessage()
                    if (errorMessage != null) {
                        updateState { controller.setError(it, errorMessage).getNewState() }
                        sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
                updateState {
                    val withError =
                        controller.setError(it, "Ошибка при обработке штрих-кода: ${e.message}")
                            .getNewState()
                    controller.setLoading(withError, false).getNewState()
                }
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка при обработке штрих-кода: ${e.message}"))
            } finally {
                isProcessingBarcode = false
                Timber.d("Сброс флага обработки сканирования после завершения")

                resetProcessingJob?.cancel()
                resetProcessingJob = null
            }
        }
    }

    fun completeAction() {
        launchIO {
            val currentState = uiState.value
            val factAction = currentState.factAction ?: return@launchIO
            val plannedAction = currentState.plannedAction ?: return@launchIO

            if (!validator.canComplete(currentState)) {
                sendEvent(ActionWizardEvent.ShowSnackbar("Не все обязательные шаги выполнены"))
                return@launchIO
            }

            try {
                updateState { controller.submitForm(it).getNewState() }

                val syncWithServer = plannedAction.actionTemplate?.syncWithServer == true
                val result = networkService.completeAction(factAction, syncWithServer)

                if (result.isSuccess()) {
                    updateState { controller.handleSendSuccess(it).getNewState() }
                    sendEvent(ActionWizardEvent.NavigateToTaskDetail)
                } else {
                    // Явно сбрасываем состояние загрузки перед установкой ошибки
                    updateState { it.copy(isLoading = false) }
                    updateState {
                        controller.handleSendFailure(
                            it,
                            result.getErrorMessage() ?: "Неизвестная ошибка"
                        ).getNewState()
                    }
                    sendEvent(
                        ActionWizardEvent.ShowSnackbar(
                            result.getErrorMessage() ?: "Не удалось отправить данные"
                        )
                    )
                }
            } catch (e: Exception) {
                // Добавляем обработку исключений с явным сбросом флага загрузки
                Timber.e(e, "Ошибка при отправке данных: ${e.message}")
                updateState { it.copy(isLoading = false, sendingFailed = true, error = e.message) }
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun showExitDialog() {
        updateState { controller.showExitDialog(it).getNewState() }
    }

    fun dismissExitDialog() {
        updateState { controller.dismissExitDialog(it).getNewState() }
    }

    fun exitWizard() {
        clearErrorAndLoading()
        updateState { controller.dismissExitDialog(it).getNewState() }
        sendEvent(ActionWizardEvent.NavigateToTaskDetail)
    }

    fun clearError() {
        isProcessingBarcode = false
        resetProcessingJob?.cancel()
        resetProcessingJob = null

        updateState { controller.clearError(it).getNewState() }
    }

    private fun determineStateType(state: ActionWizardState): WizardState {
        return when {
            state.isLoading -> WizardState.LOADING
            state.showExitDialog -> WizardState.EXIT_DIALOG
            state.showSummary -> WizardState.SUMMARY
            state.error != null -> WizardState.ERROR
            else -> WizardState.STEP
        }
    }

    fun clearErrorAndLoading() {
        isProcessingBarcode = false
        resetProcessingJob?.cancel()
        resetProcessingJob = null

        updateState {
            it.copy(
                isLoading = false,
                error = null,
                sendingFailed = false
            )
        }
    }
}