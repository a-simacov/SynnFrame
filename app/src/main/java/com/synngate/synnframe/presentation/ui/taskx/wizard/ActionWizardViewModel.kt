package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    private val taskXRepository: TaskXRepository,
    private val validationService: ValidationService,
    private val userUseCases: UserUseCases,
    private val productUseCases: ProductUseCases
) : BaseViewModel<ActionWizardState, ActionWizardEvent>(ActionWizardState(taskId = taskId, actionId = actionId)) {

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        updateState { it.copy(isLoading = true) }

        try {
            val task = TaskXDataHolderSingleton.currentTask.value
            if (task == null) {
                Timber.e("Задание не найдено в TaskXDataHolderSingleton")
                updateState { it.copy(isLoading = false, error = "Задание не найдено") }
                return
            }

            val plannedAction = task.plannedActions.find { it.id == actionId }
            if (plannedAction == null) {
                Timber.e("Действие $actionId не найдено в задании ${task.id}")
                updateState { it.copy(isLoading = false, error = "Действие не найдено") }
                return
            }

            val actionTemplate = plannedAction.actionTemplate
            if (actionTemplate == null) {
                Timber.e("Шаблон действия не найден для действия $actionId")
                updateState { it.copy(isLoading = false, error = "Шаблон действия не найден") }
                return
            }

            val sortedSteps = actionTemplate.actionSteps.sortedBy { it.order }

            val hasQuantityStep = sortedSteps.any { it.factActionField == FactActionField.QUANTITY }

            val newFactAction = FactAction(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                plannedActionId = plannedAction.id,
                actionTemplateId = actionTemplate.id,
                wmsAction = actionTemplate.wmsAction,
                quantity = if (!hasQuantityStep && plannedAction.quantity > 0) plannedAction.quantity else 0f,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now()
            )

            Timber.d("Визард успешно инициализирован для задания ${task.id}, действие $actionId")

            updateState {
                it.copy(
                    plannedAction = plannedAction,
                    steps = sortedSteps,
                    factAction = newFactAction,
                    isLoading = false
                )
            }

            plannedAction.storageProductClassifier?.let { product ->
                loadClassifierProductInfo(product.id)
            }

        } catch (e: Exception) {
            Timber.e(e, "Ошибка инициализации визарда: ${e.message}")
            updateState { it.copy(isLoading = false, error = "Ошибка: ${e.message}") }
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

    fun findProductByBarcode(barcode: String) {
        launchIO {
            try {
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    val plannedProductId = uiState.value.plannedAction?.storageProductClassifier?.id

                    if (plannedProductId == product.id) {
                        // Товар соответствует плану, можно использовать его
                        Timber.d("Найден товар по штрихкоду: $barcode, соответствует плану")
                        // Действия с найденным товаром...
                    } else {
                        Timber.w("Найденный товар не соответствует плану")
                        updateState {
                            it.copy(error = "Найденный товар не соответствует плану")
                        }
                    }
                } else {
                    Timber.w("Товар по штрихкоду $barcode не найден")
                    updateState {
                        it.copy(error = "Товар по штрихкоду не найден")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка поиска товара по штрихкоду: $barcode")
                updateState {
                    it.copy(error = "Ошибка поиска товара: ${e.message}")
                }
            }
        }
    }

    fun confirmCurrentStep() {
        val currentState = uiState.value
        val currentStepIndex = currentState.currentStepIndex
        val steps = currentState.steps

        if (currentStepIndex >= steps.size) {
            return
        }

        val currentStep = steps[currentStepIndex]

        if (!validateCurrentStep()) {
            return
        }

        if (currentStepIndex == steps.size - 1) {
            updateState { it.copy(showSummary = true) }
        } else {
            updateState { it.copy(currentStepIndex = it.currentStepIndex + 1) }
        }
    }

    fun previousStep() {
        val currentState = uiState.value

        if (currentState.showSummary) {
            updateState { it.copy(showSummary = false) }
            return
        }

        if (currentState.currentStepIndex == 0) {
            updateState { it.copy(showExitDialog = true) }
            return
        }

        updateState { it.copy(currentStepIndex = it.currentStepIndex - 1) }
    }

    fun setObjectForCurrentStep(obj: Any, autoAdvance: Boolean = true) {
        val currentState = uiState.value
        val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex) ?: return

        val updatedSelectedObjects = currentState.selectedObjects.toMutableMap()
        updatedSelectedObjects[currentStep.id] = obj

        val updatedFactAction = updateFactActionWithObject(currentState.factAction, currentStep.factActionField, obj)

        updateState {
            it.copy(
                selectedObjects = updatedSelectedObjects,
                factAction = updatedFactAction,
                error = null
            )
        }

        if (autoAdvance) {
            Timber.d("Вызываем автопереход после установки объекта: $obj для шага ${currentStep.factActionField}")
            tryAutoAdvance()
        } else {
            Timber.d("Объект установлен без автоперехода: $obj для шага ${currentStep.factActionField}")
        }
    }

    private fun tryAutoAdvance(): Boolean {
        val currentState = uiState.value
        val currentStep = currentState.getCurrentStep() ?: return false

        Timber.d("Пробуем выполнить автопереход с шага ${currentStep.name} (${currentStep.factActionField})")

        val isAdditionalPropsStep = currentState.shouldShowAdditionalProps(currentStep)

        if (isAdditionalPropsStep) {
            val taskProduct = currentState.selectedObjects[currentStep.id] as? TaskProduct
            if (taskProduct == null) {
                Timber.d("Автопереход отменен: не выбран TaskProduct для шага с доп. свойствами")
                return false
            }

            if (currentState.shouldShowExpirationDate() && taskProduct.expirationDate == null) {
                Timber.d("Автопереход отменен: не указан срок годности")
                return false
            }
        }

        if (!validateCurrentStep()) {
            Timber.d("Автопереход отменен: ошибка валидации")
            return false
        }

        if (currentState.currentStepIndex == currentState.steps.size - 1) {
            Timber.d("Автопереход: переходим к сводной информации")
            updateState { it.copy(showSummary = true) }
        } else {
            Timber.d("Автопереход: переходим к следующему шагу ${currentState.currentStepIndex + 1}")
            updateState { it.copy(currentStepIndex = it.currentStepIndex + 1) }
        }

        return true
    }

    private fun updateFactActionWithObject(factAction: FactAction?, field: FactActionField, obj: Any): FactAction? {
        if (factAction == null) return null

        return when {
            field == FactActionField.STORAGE_PRODUCT && obj is TaskProduct ->
                factAction.copy(storageProduct = obj)

            field == FactActionField.STORAGE_PRODUCT_CLASSIFIER && obj is Product ->
                factAction.copy(storageProductClassifier = obj)

            field == FactActionField.STORAGE_BIN && obj is BinX ->
                factAction.copy(storageBin = obj)

            field == FactActionField.STORAGE_PALLET && obj is Pallet ->
                factAction.copy(storagePallet = obj)

            field == FactActionField.ALLOCATION_BIN && obj is BinX ->
                factAction.copy(placementBin = obj)

            field == FactActionField.ALLOCATION_PALLET && obj is Pallet ->
                factAction.copy(placementPallet = obj)

            field == FactActionField.QUANTITY && obj is Number ->
                factAction.copy(quantity = obj.toFloat())

            else -> factAction
        }
    }

    fun validateCurrentStep(): Boolean {
        val currentState = uiState.value
        val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex) ?: return false

        if (!currentStep.isRequired) {
            return true
        }

        val stepObject = currentState.selectedObjects[currentStep.id]
        if (stepObject == null) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Необходимо выбрать объект для этого шага"))
            return false
        }

        if (currentStep.validationRules != null) {
            val validationResult = validationService.validate(
                rule = currentStep.validationRules,
                value = stepObject,
                context = mapOf("planItems" to listOfNotNull(getPlannedObjectForField(currentStep.factActionField)))
            )

            if (validationResult !is com.synngate.synnframe.domain.service.ValidationResult.Success) {
                val errorMessage = if (validationResult is com.synngate.synnframe.domain.service.ValidationResult.Error) {
                    validationResult.message
                } else {
                    "Ошибка валидации"
                }
                sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                updateState { it.copy(error = errorMessage) }
                return false
            }
        }

        return true
    }

    private fun getPlannedObjectForField(field: FactActionField): Any? {
        val plannedAction = uiState.value.plannedAction ?: return null
        val currentStep = uiState.value.steps.getOrNull(uiState.value.currentStepIndex)

        return when (field) {
            FactActionField.STORAGE_PRODUCT -> {
                // Если storageProduct есть, возвращаем его
                plannedAction.storageProduct ?: run {
                    // Если storageProduct нет, но есть storageProductClassifier и включен
                    // признак inputAdditionalProps, создаем временный TaskProduct
                    if (plannedAction.storageProductClassifier != null &&
                        currentStep?.inputAdditionalProps == true) {

                        // Создаем временный TaskProduct на основе storageProductClassifier
                        // Значения expirationDate и status не важны, так как при валидации
                        // сравнивается только product.id, а не другие поля
                        TaskProduct(
                            id = UUID.randomUUID().toString(),
                            product = plannedAction.storageProductClassifier,
                            expirationDate = null,
                            status = ProductStatus.STANDARD
                        )
                    } else {
                        null
                    }
                }
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> plannedAction.storageProductClassifier
            FactActionField.STORAGE_BIN -> plannedAction.storageBin
            FactActionField.STORAGE_PALLET -> plannedAction.storagePallet
            FactActionField.ALLOCATION_BIN -> plannedAction.placementBin
            FactActionField.ALLOCATION_PALLET -> plannedAction.placementPallet
            FactActionField.QUANTITY -> plannedAction.quantity
            else -> null
        }
    }

    fun completeAction() {
        launchIO {
            val currentState = uiState.value
            val factAction = currentState.factAction ?: return@launchIO
            val plannedAction = currentState.plannedAction ?: return@launchIO

            updateState { it.copy(isLoading = true, sendingFailed = false) }

            try {
                val updatedFactAction = factAction.copy(completedAt = LocalDateTime.now())

                if (plannedAction.actionTemplate?.syncWithServer == true) {
                    val endpoint = TaskXDataHolderSingleton.endpoint ?: ""
                    val result = taskXRepository.addFactAction(updatedFactAction, endpoint)

                    if (result.isSuccess) {
                        val updatedTask = result.getOrNull()
                        if (updatedTask != null) {
                            TaskXDataHolderSingleton.updateTask(updatedTask)
                        }

                        sendEvent(ActionWizardEvent.NavigateToTaskDetail)
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                sendingFailed = true,
                                error = "Ошибка отправки: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    }
                } else {
                    TaskXDataHolderSingleton.addFactAction(updatedFactAction)
                    sendEvent(ActionWizardEvent.NavigateToTaskDetail)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка завершения действия: ${e.message}")
                updateState {
                    it.copy(
                        isLoading = false,
                        sendingFailed = true,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun showExitDialog() {
        updateState { it.copy(showExitDialog = true) }
    }

    fun dismissExitDialog() {
        updateState { it.copy(showExitDialog = false) }
    }

    fun exitWizard() {
        updateState { it.copy(showExitDialog = false) }
        sendEvent(ActionWizardEvent.NavigateToTaskDetail)
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    /**
     * Публичный метод для обработки штрих-кодов из UI.
     * Может вызываться из Composable функций.
     *
     * @param barcode Штрих-код для поиска
     * @param fieldType Тип поля, для которого выполняется поиск. Если null - будет использоваться тип текущего шага.
     */
    fun handleBarcode(barcode: String, fieldType: FactActionField? = null) {
        val actualFieldType = fieldType ?: uiState.value.getCurrentStep()?.factActionField

        if (barcode.isBlank()) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Пустой штрихкод"))
            return
        }

        if (actualFieldType == null) {
            Timber.w("Тип поля не определен для штрих-кода: $barcode")
            sendEvent(ActionWizardEvent.ShowSnackbar("Не удалось определить тип поля для поиска"))
            return
        }

        Timber.d("Обработка штрих-кода: $barcode для поля типа: $actualFieldType")

        updateState { it.copy(error = null) }

        viewModelScope.launch {
            try {
                updateState { it.copy(isLoading = true) }

                val result = when (actualFieldType) {
                    FactActionField.STORAGE_BIN -> searchBin(barcode, true)
                    FactActionField.ALLOCATION_BIN -> searchBin(barcode, false)
                    FactActionField.STORAGE_PALLET -> searchPallet(barcode, true)
                    FactActionField.ALLOCATION_PALLET -> searchPallet(barcode, false)
                    FactActionField.STORAGE_PRODUCT -> searchTaskProduct(barcode)
                    FactActionField.STORAGE_PRODUCT_CLASSIFIER -> searchProductClassifier(barcode)
                    FactActionField.QUANTITY -> {
                        barcode.toFloatOrNull()?.let {
                            setObjectForCurrentStep(it, true)
                            true
                        } ?: false
                    }
                }

                updateState { it.copy(isLoading = false) }

                if (!result) {
                    if (uiState.value.error == null) {
                        updateState { it.copy(error = "Не удалось найти объект по штрих-коду: $barcode") }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при обработке штрих-кода: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun searchProductClassifier(barcode: String): Boolean {
        try {
            val currentStep = uiState.value.getCurrentStep() ?: return false
            val plannedProduct = uiState.value.plannedAction?.storageProductClassifier

            if (plannedProduct != null) {
                if (plannedProduct.id == barcode) {
                    setObjectForCurrentStep(plannedProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар по ID: ${plannedProduct.id}")
                    return true
                }

                if (plannedProduct.articleNumber == barcode) {
                    setObjectForCurrentStep(plannedProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар по артикулу: ${plannedProduct.articleNumber}")
                    return true
                }

                val foundByBarcode = plannedProduct.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    setObjectForCurrentStep(plannedProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар по штрих-коду: $barcode")
                    return true
                }
            }

            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                if (validateFoundObject(product, currentStep)) {
                    setObjectForCurrentStep(product, autoAdvance = true)
                    Timber.d("Найден товар по штрих-коду: $barcode")
                    return true
                } else {
                    updateState {
                        it.copy(error = "Найденный товар не соответствует плану")
                    }
                    return false
                }
            }

            val productById = productUseCases.getProductById(barcode)

            if (productById != null) {
                if (validateFoundObject(productById, currentStep)) {
                    setObjectForCurrentStep(productById, autoAdvance = true)
                    Timber.d("Найден товар по ID: $barcode")
                    return true
                } else {
                    updateState {
                        it.copy(error = "Найденный товар не соответствует плану")
                    }
                    return false
                }
            }

            updateState {
                it.copy(error = "Товар не найден по штрихкоду или ID: $barcode")
            }
            return false

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара классификатора: $barcode")
            updateState {
                it.copy(error = "Ошибка поиска: ${e.message}")
            }
            return false
        }
    }

    private suspend fun searchTaskProduct(barcode: String): Boolean {
        try {
            val currentStep = uiState.value.getCurrentStep() ?: return false
            val plannedTaskProduct = uiState.value.plannedAction?.storageProduct
            val plannedClassifierProduct = uiState.value.plannedAction?.storageProductClassifier

            if (plannedTaskProduct != null) {
                if (plannedTaskProduct.id == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар задания по ID: ${plannedTaskProduct.id}")
                    return true
                }

                if (plannedTaskProduct.product.id == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар задания по ID товара: ${plannedTaskProduct.product.id}")
                    return true
                }

                if (plannedTaskProduct.product.articleNumber == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар задания по артикулу: ${plannedTaskProduct.product.articleNumber}")
                    return true
                }

                val foundByBarcode = plannedTaskProduct.product.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    setObjectForCurrentStep(plannedTaskProduct, autoAdvance = true)
                    Timber.d("Найден запланированный товар задания по штрих-коду: $barcode")
                    return true
                }
            }

            // Особый случай: если нет запланированного товара задания, но есть товар классификатора
            // и текущий шаг поддерживает дополнительные свойства (срок годности, статус и т.д.)
            if (plannedTaskProduct == null && plannedClassifierProduct != null &&
                uiState.value.shouldShowAdditionalProps(currentStep)) {

                if (plannedClassifierProduct.id == barcode ||
                    plannedClassifierProduct.articleNumber == barcode ||
                    plannedClassifierProduct.units.any { unit ->
                        unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                    }) {

                    val taskProduct = uiState.value.getTaskProductFromClassifier(currentStep.id)

                    if (validateFoundObject(taskProduct, currentStep)) {
                        // Для товара с дополнительными свойствами не делаем автопереход,
                        // так как нужно заполнить дополнительные поля
                        setObjectForCurrentStep(taskProduct, autoAdvance = false)
                        Timber.d("Создан товар задания на основе товара классификатора: $barcode")
                        return true
                    } else {
                        updateState {
                            it.copy(error = "Найденный товар не соответствует плану")
                        }
                        return false
                    }
                }
            }

            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                val taskProduct = TaskProduct(
                    id = UUID.randomUUID().toString(),
                    product = product,
                    status = ProductStatus.STANDARD
                )

                if (validateFoundObject(taskProduct, currentStep)) {
                    val autoAdvance = !uiState.value.shouldShowAdditionalProps(currentStep)
                    setObjectForCurrentStep(taskProduct, autoAdvance = autoAdvance)
                    Timber.d("Найден товар по штрих-коду и создан товар задания: $barcode")
                    return true
                } else {
                    updateState {
                        it.copy(error = "Найденный товар не соответствует плану")
                    }
                    return false
                }
            }

            updateState {
                it.copy(error = "Товар не найден по штрихкоду или ID: $barcode")
            }
            return false

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара задания: $barcode")
            updateState {
                it.copy(error = "Ошибка поиска: ${e.message}")
            }
            return false
        }
    }

    private suspend fun searchBin(barcode: String, isStorage: Boolean): Boolean {
        try {
            val currentStep = uiState.value.getCurrentStep() ?: return false

            val plannedBin = if (isStorage) {
                uiState.value.plannedAction?.storageBin
            } else {
                uiState.value.plannedAction?.placementBin
            }

            if (plannedBin != null && plannedBin.code == barcode) {
                setObjectForCurrentStep(plannedBin, autoAdvance = true)
                Timber.d("Найдена запланированная ячейка: ${plannedBin.code}")
                return true
            }

            val bin = BinX(code = barcode, zone = "")

            val isValid = validateFoundObject(bin, currentStep)

            if (isValid) {
                Timber.d("Ячейка $barcode прошла валидацию, устанавливаем объект")
                setObjectForCurrentStep(bin, autoAdvance = true)
                return true
            } else {
                Timber.d("Ячейка $barcode НЕ прошла валидацию")
                updateState { it.copy(error = "Введенная ячейка не соответствует плану") }
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячейки: $barcode")
            updateState { it.copy(error = "Ошибка поиска: ${e.message}") }
            return false
        }
    }

    private suspend fun searchPallet(barcode: String, isStorage: Boolean): Boolean {
        try {
            val currentStep = uiState.value.getCurrentStep() ?: return false

            val plannedPallet = if (isStorage) {
                uiState.value.plannedAction?.storagePallet
            } else {
                uiState.value.plannedAction?.placementPallet
            }

            if (plannedPallet != null && plannedPallet.code == barcode) {
                setObjectForCurrentStep(plannedPallet, autoAdvance = true)
                Timber.d("Найдена запланированная паллета: ${plannedPallet.code}")
                return true
            }

            val pallet = Pallet(code = barcode)

            val isValid = validateFoundObject(pallet, currentStep)

            if (isValid) {
                Timber.d("Паллета $barcode прошла валидацию, устанавливаем объект")
                setObjectForCurrentStep(pallet, autoAdvance = true)
                return true
            } else {
                Timber.d("Паллета $barcode НЕ прошла валидацию")
                updateState { it.copy(error = "Введенная паллета не соответствует плану") }
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллеты: $barcode")
            updateState { it.copy(error = "Ошибка поиска: ${e.message}") }
            return false
        }
    }

    private fun validateFoundObject(obj: Any, step: ActionStepTemplate): Boolean {
        if (step.validationRules == null) {
            return true
        }

        val planItem = getPlannedObjectForField(step.factActionField)
        val context = if (planItem != null) {
            mapOf("planItems" to listOf(planItem))
        } else {
            emptyMap()
        }

        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return validationResult.isSuccess
    }

    fun setLoading(isLoading: Boolean) {
        updateState { it.copy(isLoading = isLoading) }
    }

    fun setError(message: String?) {
        updateState { it.copy(error = message) }
    }
}