package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.ObjectSearchService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardController
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardNetworkService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardValidator
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
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

    // Сервисы для работы с визардом
    private val stateMachine = WizardStateMachine()
    private val validator = WizardValidator(validationService)
    private val controller = WizardController(validator, stateMachine)
    private val objectSearchService = ObjectSearchService(productUseCases, validator)
    private val networkService = WizardNetworkService(taskXRepository)

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        updateState { it.copy(isLoading = true) }

        val initialState = controller.initializeWizard(taskId, actionId)

        updateState { initialState }

        // Загружаем дополнительную информацию о товаре, если необходимо
        initialState.plannedAction?.storageProductClassifier?.let { product ->
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
        val newState = controller.confirmCurrentStep(uiState.value) {
            validateCurrentStep()
        }
        updateState { newState }
    }

    fun previousStep() {
        val newState = controller.previousStep(uiState.value)
        updateState { newState }
    }

    fun setObjectForCurrentStep(obj: Any, autoAdvance: Boolean = true) {
        val updatedState = controller.setObjectForCurrentStep(uiState.value, obj)
        updateState { updatedState }

        if (autoAdvance) {
            Timber.d("Вызываем автопереход после установки объекта: $obj")
            tryAutoAdvance()
        } else {
            Timber.d("Объект установлен без автоперехода: $obj")
        }
    }

    private fun tryAutoAdvance() {
        val (success, newState) = controller.tryAutoAdvance(uiState.value) {
            validateCurrentStep()
        }

        if (success) {
            updateState { newState }
        }
    }

    private fun validateCurrentStep(): Boolean {
        val isValid = validator.validateCurrentStep(uiState.value)

        if (!isValid) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Необходимо заполнить все обязательные поля"))
        }

        return isValid
    }

    fun handleBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                updateState { controller.setLoading(it, true) }

                val (success, foundObject, errorMessage) = objectSearchService.handleBarcode(uiState.value, barcode)

                if (success && foundObject != null) {
                    // Сначала снимаем флаг загрузки
                    updateState { controller.setLoading(it, false) }
                    // Затем устанавливаем найденный объект и пробуем выполнить автопереход
                    setObjectForCurrentStep(foundObject, true)
                } else {
                    // Если получена ошибка, обрабатываем её
                    if (errorMessage != null) {
                        updateState {
                            controller.setLoading(
                                controller.setError(it, errorMessage),
                                false
                            )
                        }
                        sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                    } else {
                        // Если ошибки нет (например, при повторном сканировании), просто снимаем флаг загрузки
                        updateState { controller.setLoading(it, false) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
                updateState {
                    controller.setLoading(
                        controller.setError(it, "Ошибка при обработке штрих-кода: ${e.message}"),
                        false
                    )
                }
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка при обработке штрих-кода: ${e.message}"))
            }
        }
    }

    fun completeAction() {
        launchIO {
            val currentState = uiState.value
            val factAction = currentState.factAction ?: return@launchIO
            val plannedAction = currentState.plannedAction ?: return@launchIO

            // Используем контроллер для перехода в состояние отправки
            updateState { controller.submitForm(it) }

            val syncWithServer = plannedAction.actionTemplate?.syncWithServer == true
            val (success, errorMessage) = networkService.completeAction(factAction, syncWithServer)

            if (success) {
                // Используем контроллер для обработки успешной отправки
                updateState { controller.handleSendSuccess(it) }
                sendEvent(ActionWizardEvent.NavigateToTaskDetail)
            } else {
                // Используем контроллер для обработки ошибки отправки
                updateState {
                    controller.handleSendFailure(it, errorMessage ?: "Неизвестная ошибка")
                }
                sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage ?: "Не удалось отправить данные"))
            }
        }
    }

    fun showExitDialog() {
        updateState { controller.showExitDialog(it) }
    }

    fun dismissExitDialog() {
        updateState { controller.dismissExitDialog(it) }
    }

    fun exitWizard() {
        updateState { controller.dismissExitDialog(it) }
        sendEvent(ActionWizardEvent.NavigateToTaskDetail)
    }

    fun clearError() {
        updateState { controller.clearError(it) }
    }
}