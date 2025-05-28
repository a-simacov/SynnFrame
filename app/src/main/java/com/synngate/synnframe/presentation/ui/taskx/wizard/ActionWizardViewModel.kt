package com.synngate.synnframe.presentation.ui.taskx.wizard

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

    fun setObjectForCurrentStep(obj: Any) {
        val currentState = uiState.value
        val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex) ?: return

        val updatedSelectedObjects = currentState.selectedObjects.toMutableMap()
        updatedSelectedObjects[currentStep.id] = obj

        val updatedFactAction = updateFactActionWithObject(currentState.factAction, currentStep.factActionField, obj)

        updateState {
            it.copy(
                selectedObjects = updatedSelectedObjects,
                factAction = updatedFactAction
            )
        }
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

    fun searchObjectByBarcode(barcode: String, fieldType: FactActionField) {
        if (barcode.isBlank()) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Пустой штрихкод"))
            return
        }

        // Добавляем логирование для отладки
        Timber.d("Поиск объекта по штрихкоду: $barcode для поля типа: $fieldType")

        updateState { it.copy(isLoading = true, error = null) }

        launchIO {
            try {
                when (fieldType) {
                    FactActionField.STORAGE_PRODUCT_CLASSIFIER -> searchProductClassifier(barcode)
                    FactActionField.STORAGE_PRODUCT -> searchTaskProduct(barcode)
                    FactActionField.STORAGE_BIN -> searchBin(barcode, isStorage = true)
                    FactActionField.ALLOCATION_BIN -> searchBin(barcode, isStorage = false)
                    FactActionField.STORAGE_PALLET -> searchPallet(barcode, isStorage = true)
                    FactActionField.ALLOCATION_PALLET -> searchPallet(barcode, isStorage = false)
                    else -> {
                        updateState { it.copy(isLoading = false) }
                        sendEvent(ActionWizardEvent.ShowSnackbar("Тип поля не поддерживает поиск"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске объекта по штрихкоду: $barcode для типа: $fieldType")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка поиска: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Поиск товара из классификатора по штрихкоду, ID или артикулу
     */
    private suspend fun searchProductClassifier(barcode: String) {
        try {
            // Получаем текущий шаг
            val currentStep = uiState.value.getCurrentStep() ?: return
            val plannedProduct = uiState.value.plannedAction?.storageProductClassifier

            // Сначала проверяем, совпадает ли баркод с информацией из плана
            if (plannedProduct != null) {
                // Проверяем по ID
                if (plannedProduct.id == barcode) {
                    setObjectForCurrentStep(plannedProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }

                // Проверяем по артикулу
                if (plannedProduct.articleNumber == barcode) {
                    setObjectForCurrentStep(plannedProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }

                // Проверка по штрихкодам через основные единицы измерения
                val foundByBarcode = plannedProduct.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    setObjectForCurrentStep(plannedProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }
            }

            // Если не нашли в плане или плана нет, ищем в базе данных по штрихкоду
            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                // Проверяем валидацию
                if (validateFoundObject(product, currentStep)) {
                    setObjectForCurrentStep(product)
                    updateState { it.copy(isLoading = false) }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Найденный товар не соответствует плану"
                        )
                    }
                }
                return
            }

            // Если не нашли по штрихкоду, пробуем найти по ID
            val productById = productUseCases.getProductById(barcode)

            if (productById != null) {
                // Проверяем валидацию
                if (validateFoundObject(productById, currentStep)) {
                    setObjectForCurrentStep(productById)
                    updateState { it.copy(isLoading = false) }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Найденный товар не соответствует плану"
                        )
                    }
                }
                return
            }

            // Если ничего не нашли
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Товар не найден по штрихкоду или ID: $barcode"
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара классификатора: $barcode")
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    /**
     * Поиск товара задания по штрихкоду, ID или артикулу
     */
    private suspend fun searchTaskProduct(barcode: String) {
        try {
            // Получаем текущий шаг
            val currentStep = uiState.value.getCurrentStep() ?: return
            val plannedTaskProduct = uiState.value.plannedAction?.storageProduct
            val plannedClassifierProduct = uiState.value.plannedAction?.storageProductClassifier

            // Сначала проверяем, совпадает ли баркод с информацией из плана
            if (plannedTaskProduct != null) {
                // Проверяем по ID товара задания
                if (plannedTaskProduct.id == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }

                // Проверяем по ID товара классификатора
                if (plannedTaskProduct.product.id == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }

                // Проверяем по артикулу
                if (plannedTaskProduct.product.articleNumber == barcode) {
                    setObjectForCurrentStep(plannedTaskProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }

                // Проверка по штрихкодам через основные единицы измерения
                val foundByBarcode = plannedTaskProduct.product.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    setObjectForCurrentStep(plannedTaskProduct)
                    updateState { it.copy(isLoading = false) }
                    return
                }
            }

            // Если в плане есть товар из классификатора, но нет товара задания,
            // и нужно заполнить дополнительные свойства
            if (plannedTaskProduct == null && plannedClassifierProduct != null &&
                uiState.value.shouldShowAdditionalProps(currentStep)) {

                // Проверяем, совпадает ли с товаром классификатора
                if (plannedClassifierProduct.id == barcode ||
                    plannedClassifierProduct.articleNumber == barcode ||
                    plannedClassifierProduct.units.any { unit ->
                        unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                    }) {

                    // Создаем TaskProduct на основе товара из классификатора
                    val taskProduct = uiState.value.getTaskProductFromClassifier(currentStep.id)

                    // Проверяем валидацию
                    if (validateFoundObject(taskProduct, currentStep)) {
                        setObjectForCurrentStep(taskProduct)
                        updateState { it.copy(isLoading = false) }
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Найденный товар не соответствует плану"
                            )
                        }
                    }
                    return
                }
            }

            // Если не нашли в плане или плана нет, ищем в базе данных
            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                // Создаем TaskProduct на основе найденного товара
                val taskProduct = TaskProduct(
                    id = UUID.randomUUID().toString(),
                    product = product,
                    status = ProductStatus.STANDARD
                )

                // Проверяем валидацию
                if (validateFoundObject(taskProduct, currentStep)) {
                    setObjectForCurrentStep(taskProduct)
                    updateState { it.copy(isLoading = false) }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Найденный товар не соответствует плану"
                        )
                    }
                }
                return
            }

            // Если ничего не нашли
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Товар не найден по штрихкоду или ID: $barcode"
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара задания: $barcode")
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    /**
     * Поиск ячейки по коду
     */
    private fun searchBin(barcode: String, isStorage: Boolean) {
        try {
            // Получаем текущий шаг
            val currentStep = uiState.value.getCurrentStep() ?: return

            // Проверяем, есть ли ячейка в плане
            val plannedBin = if (isStorage) {
                uiState.value.plannedAction?.storageBin
            } else {
                uiState.value.plannedAction?.placementBin
            }

            // Сначала проверяем, совпадает ли баркод с информацией из плана
            if (plannedBin != null && plannedBin.code == barcode) {
                setObjectForCurrentStep(plannedBin)
                updateState { it.copy(isLoading = false) }
                return
            }

            // Если не нашли в плане или плана нет, создаем новую ячейку
            val bin = BinX(code = barcode, zone = "")

            // Проверяем валидацию
            if (validateFoundObject(bin, currentStep)) {
                setObjectForCurrentStep(bin)
                updateState { it.copy(isLoading = false) }
            } else {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Введенная ячейка не соответствует плану"
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячейки: $barcode")
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    /**
     * Поиск паллеты по коду
     */
    private fun searchPallet(barcode: String, isStorage: Boolean) {
        try {
            // Получаем текущий шаг
            val currentStep = uiState.value.getCurrentStep() ?: return

            // Проверяем, есть ли паллета в плане
            val plannedPallet = if (isStorage) {
                uiState.value.plannedAction?.storagePallet
            } else {
                uiState.value.plannedAction?.placementPallet
            }

            // Сначала проверяем, совпадает ли баркод с информацией из плана
            if (plannedPallet != null && plannedPallet.code == barcode) {
                setObjectForCurrentStep(plannedPallet)
                updateState { it.copy(isLoading = false) }
                return
            }

            // Если не нашли в плане или плана нет, создаем новую паллету
            val pallet = Pallet(code = barcode)

            // Проверяем валидацию
            if (validateFoundObject(pallet, currentStep)) {
                setObjectForCurrentStep(pallet)
                updateState { it.copy(isLoading = false) }
            } else {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Введенная паллета не соответствует плану"
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллеты: $barcode")
            updateState {
                it.copy(
                    isLoading = false,
                    error = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    /**
     * Валидирует найденный объект с помощью правил валидации текущего шага
     */
    private fun validateFoundObject(obj: Any, step: ActionStepTemplate): Boolean {
        // Если у шага нет правил валидации, считаем объект валидным
        if (step.validationRules == null) {
            return true
        }

        // Получаем плановый объект для формирования контекста валидации
        val planItem = getPlannedObjectForField(step.factActionField)
        val context = if (planItem != null) {
            mapOf("planItems" to listOf(planItem))
        } else {
            emptyMap()
        }

        // Выполняем валидацию
        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return validationResult.isSuccess
    }

    /**
     * Проверяет, нужно ли отображать форму ввода дополнительных свойств товара
     */
    fun ActionWizardState.shouldShowAdditionalProps(step: ActionStepTemplate): Boolean {
        return plannedAction?.storageProductClassifier != null &&
                plannedAction.storageProduct == null &&
                step.inputAdditionalProps &&
                step.factActionField == FactActionField.STORAGE_PRODUCT
    }
}