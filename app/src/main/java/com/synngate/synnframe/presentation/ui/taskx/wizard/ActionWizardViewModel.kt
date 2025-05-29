package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
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
    private val userUseCases: UserUseCases,
    private val productUseCases: ProductUseCases
) : BaseViewModel<ActionWizardState, ActionWizardEvent>(ActionWizardState(taskId = taskId, actionId = actionId)) {

    // Фабрика обработчиков полей
    private val handlerFactory = FieldHandlerFactory(validationService, productUseCases)

    // Сервисы для работы с визардом
    private val stateMachine = WizardStateMachine()
    private val validator = WizardValidator(validationService, handlerFactory)
    private val controller = WizardController(validator, stateMachine)
    private val objectSearchService = ObjectSearchService(productUseCases, validationService)
    private val networkService = WizardNetworkService(taskXRepository)

    // Флаг для отслеживания текущей обработки сканирования
    private var isProcessingBarcode = false

    // Задача для автоматического сброса флага обработки
    private var resetProcessingJob: Job? = null

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        updateState { it.copy(isLoading = true) }

        val initResult = controller.initializeWizard(taskId, actionId)
        updateState { initResult.getNewState() }

        // Загружаем дополнительную информацию о товаре, если необходимо
        initResult.getNewState().plannedAction?.storageProductClassifier?.let { product ->
            loadClassifierProductInfo(product.id)
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

                    Timber.d("Загружена полная информация о товаре классификатора: $productId, модель учета: ${product.accountingModel}")
                } else {
                    updateState {
                        it.copy(
                            isLoadingProductInfo = false,
                            productInfoError = "Товар $productId не найден в базе данных"
                        )
                    }

                    Timber.w("Товар классификатора $productId не найден в базе данных")
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoadingProductInfo = false,
                        productInfoError = "Ошибка загрузки данных о товаре: ${e.message}"
                    )
                }

                Timber.e(e, "Ошибка загрузки данных о товаре классификатора: $productId")
            }
        }
    }

    fun confirmCurrentStep() {
        launchIO {
            val result = controller.confirmCurrentStep(uiState.value) {
                validateCurrentStep()
            }
            updateState { result.getNewState() }
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
        } else {
            Timber.d("Объект установлен без автоперехода: $obj")
        }
    }

    private fun tryAutoAdvance() {
        launchIO {
            val result = controller.tryAutoAdvance(uiState.value) {
                validateCurrentStep()
            }

            if (result.isSuccess()) {
                updateState { result.getNewState() }
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
        // Игнорируем сканирование, если мы находимся на экране сводки или диалоге выхода
        val currentState = determineStateType(uiState.value)
        if (currentState == WizardState.SUMMARY ||
            currentState == WizardState.EXIT_DIALOG ||
            currentState == WizardState.SENDING) {
            Timber.d("Игнорирование сканирования в состоянии $currentState")
            return
        }

        // Проверяем, не обрабатывается ли уже другое сканирование
        if (isProcessingBarcode) {
            Timber.d("Игнорирование сканирования: предыдущее еще обрабатывается")
            return
        }

        // Отменяем предыдущую задачу сброса флага, если она еще выполняется
        resetProcessingJob?.cancel()

        // Устанавливаем флаг, что начали обработку
        isProcessingBarcode = true

        // Показываем индикатор загрузки
        updateState { controller.setLoading(it, true).getNewState() }

        // Запускаем задачу для гарантированного сброса флага обработки через 5 секунд
        // в любом случае (это страховка на случай сбоев)
        resetProcessingJob = viewModelScope.launch {
            delay(5000)
            if (isProcessingBarcode) {
                Timber.d("Принудительный сброс флага обработки сканирования по таймауту")
                isProcessingBarcode = false
                updateState { controller.setLoading(it, false).getNewState() } // Также сбрасываем состояние загрузки
            }
        }

        // Запускаем задачу для обработки сканирования
        viewModelScope.launch {
            try {
                val result = objectSearchService.handleBarcode(uiState.value, barcode)

                // Снимаем флаг загрузки
                updateState { controller.setLoading(it, false).getNewState() }

                if (result.isSuccess() && result.isProcessingRequired()) {
                    // Устанавливаем найденный объект и пробуем выполнить автопереход
                    val foundObject = result.getResultData()
                    if (foundObject != null) {
                        setObjectForCurrentStep(foundObject, true)
                    }
                } else if (!result.isSuccess()) {
                    // Если получена ошибка, обрабатываем её
                    val errorMessage = result.getErrorMessage()
                    if (errorMessage != null) {
                        updateState { controller.setError(it, errorMessage).getNewState() }
                        sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
                updateState {
                    val withError = controller.setError(it, "Ошибка при обработке штрих-кода: ${e.message}").getNewState()
                    controller.setLoading(withError, false).getNewState()
                }
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка при обработке штрих-кода: ${e.message}"))
            } finally {
                // Безусловно сбрасываем флаг обработки в блоке finally
                isProcessingBarcode = false
                Timber.d("Сброс флага обработки сканирования после завершения")

                // Отменяем таймер автоматического сброса
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

            // Проверяем, что визард можно завершить
            if (!validator.canComplete(currentState)) {
                sendEvent(ActionWizardEvent.ShowSnackbar("Не все обязательные шаги выполнены"))
                return@launchIO
            }

            // Используем контроллер для перехода в состояние отправки
            updateState { controller.submitForm(it).getNewState() }

            val syncWithServer = plannedAction.actionTemplate?.syncWithServer == true
            val result = networkService.completeAction(factAction, syncWithServer)

            if (result.isSuccess()) {
                // Используем контроллер для обработки успешной отправки
                updateState { controller.handleSendSuccess(it).getNewState() }
                sendEvent(ActionWizardEvent.NavigateToTaskDetail)
            } else {
                // Используем контроллер для обработки ошибки отправки
                updateState {
                    controller.handleSendFailure(it, result.getErrorMessage() ?: "Неизвестная ошибка").getNewState()
                }
                sendEvent(ActionWizardEvent.ShowSnackbar(result.getErrorMessage() ?: "Не удалось отправить данные"))
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
        updateState { controller.dismissExitDialog(it).getNewState() }
        sendEvent(ActionWizardEvent.NavigateToTaskDetail)
    }

    fun clearError() {
        // Сбрасываем флаг обработки при очистке ошибки
        isProcessingBarcode = false
        resetProcessingJob?.cancel()
        resetProcessingJob = null

        updateState { controller.clearError(it).getNewState() }
    }

    /**
     * Определяет текущее состояние визарда
     */
    private fun determineStateType(state: ActionWizardState): WizardState {
        return when {
            state.isLoading -> WizardState.LOADING
            state.showExitDialog -> WizardState.EXIT_DIALOG
            state.showSummary -> WizardState.SUMMARY
            state.error != null -> WizardState.ERROR
            else -> WizardState.STEP
        }
    }
}