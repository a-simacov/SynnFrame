package com.synngate.synnframe.presentation.ui.taskx.wizard

import com.synngate.synnframe.data.remote.dto.CommandNextAction
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.CommandExecutionStatus
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.CommandExecutionResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.ExpressionEvaluator
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.ObjectSearchService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardController
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardNetworkService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardValidator
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.UUID

class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    validationService: ValidationService,
    private val productUseCases: ProductUseCases,
    private val networkService: WizardNetworkService
) : BaseViewModel<ActionWizardState, ActionWizardEvent>(
    ActionWizardState(
        taskId = taskId,
        actionId = actionId
    )
) {

    private val handlerFactory = FieldHandlerFactory(validationService, productUseCases)
    private val expressionEvaluator = ExpressionEvaluator()
    private val stateMachine = WizardStateMachine()
    private val validator = WizardValidator(validationService, handlerFactory, expressionEvaluator)
    private val controller = WizardController(stateMachine, expressionEvaluator)
    private val objectSearchService = ObjectSearchService(productUseCases, validationService)

    // Добавить новое состояние для команд
    private var executingCommands = mutableSetOf<String>()

    private var isProcessingBarcode = false
    private var resetProcessingJob: Job? = null

    // Новый Job для запроса объекта с сервера
    private var serverRequestJob: Job? = null
    
    // Job для запроса обновлений полей с сервера
    private var fieldUpdateJob: Job? = null
    
    // Флаг для предотвращения множественных валидаций
    private var isValidating = false

    // Новый callback для навигации
    private var navigateBackCallback: (() -> Unit)? = null

    // Метод для установки callback'а навигации
    fun setNavigateBackCallback(callback: () -> Unit) {
        navigateBackCallback = callback
        Timber.d("Back navigation callback set from wizard")
    }

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        updateState { it.copy(isLoading = true) }

        objectSearchService.clearCache()

        val initResult = controller.initializeWizard(taskId, actionId)
        val initialState = initResult.getNewState()

        if (initialState.error != null) {
            updateState { initialState }
            return
        }

        if (initialState.showSummary) {
            updateState { initialState }
            return
        }

        updateState { initialState }

        initialState.plannedAction?.storageProductClassifier?.let { product ->
            loadClassifierProductInfo(product.id)
        }

        if (initialState.currentStepIndex >= 0 && initialState.currentStepIndex < initialState.steps.size) {
            initializeStepWithFiltersAndBuffer(initialState.currentStepIndex)
        }
    }

    /**
     * Инициализация шага: проверка и применение значений из буфера и фильтров
     */
    private fun initializeStepWithFiltersAndBuffer(stepIndex: Int) {
        val state = uiState.value
        if (stepIndex < 0 || stepIndex >= state.steps.size) return

        launchIO {
            // 1. Проверяем видимость текущего шага
            val stepToCheck = state.steps[stepIndex]
            if (!expressionEvaluator.evaluateVisibilityCondition(stepToCheck.visibilityCondition, state)) {
                Timber.d("Step ${stepToCheck.id} (${stepToCheck.name}) is invisible, looking for the next visible step")
                // Если шаг невидим, ищем следующий видимый
                val nextVisibleIndex = expressionEvaluator.findNextVisibleStepIndex(state, stepIndex)
                if (nextVisibleIndex != null) {
                    Timber.d("Found next visible step with index $nextVisibleIndex")
                    updateState { it.copy(currentStepIndex = nextVisibleIndex) }
                    // Рекурсивно инициализируем следующий видимый шаг
                    initializeStepWithFiltersAndBuffer(nextVisibleIndex)
                } else {
                    Timber.d("No visible steps ahead, proceeding to the summary screen")
                    // Если нет видимых шагов впереди, переходим к экрану итогов
                    updateState { it.copy(showSummary = true) }
                }
                return@launchIO
            }

            // 2. Сначала проверяем и применяем значения из буфера
            val updatedState = uiState.value // Получаем актуальное состояние после возможного изменения
            val applyBufferResult = controller.applyBufferValueIfNeeded(updatedState)
            val stateAfterBuffer = applyBufferResult.getNewState()
            updateState { stateAfterBuffer }

            // Проверяем, было ли изменено состояние после применения буфера
            val bufferApplied = stateAfterBuffer != updatedState

            // 3. Затем проверяем и применяем значения из фильтров (если есть и буфер не применился)
            if (!bufferApplied) {
                val activeFilters = TaskXDataHolderSingleton.actionsFilter.getActiveFilters()

                // Получаем текущий шаг из актуального состояния
                val currentStepAfterBuffer = stateAfterBuffer.getCurrentStep()

                if (currentStepAfterBuffer != null) {
                    // Ищем фильтр, соответствующий текущему шагу
                    val matchingFilter = activeFilters.find { it.field == currentStepAfterBuffer.factActionField }

                    if (matchingFilter != null) {
                        Timber.d("Found matching filter for step ${currentStepAfterBuffer.id}: ${matchingFilter.field}")

                        // Устанавливаем значение из фильтра
                        setObjectForCurrentStep(matchingFilter.data, true)

                        // Для логирования
                        Timber.d("Value from filter ${matchingFilter.field} set to step ${currentStepAfterBuffer.id}")
                        return@launchIO
                    }
                }
            }

            // 4. Если объект из буфера успешно применен, выполняем автопереход для заблокированных полей
            val finalState = uiState.value // Получаем окончательное состояние после всех изменений
            if (bufferApplied && finalState.isCurrentStepLockedByBuffer()) {
                Timber.d("Performing auto-advance from buffer for locked field")
                delay(300) // Небольшая задержка для UI
                tryAutoAdvanceFromBuffer()
            }

            // 5. Если для шага настроен serverSelectionEndpoint, автоматически запрашиваем объект с сервера
            val stateForServerRequest = uiState.value
            val currentStepForServerRequest = stateForServerRequest.getCurrentStep()

            if (currentStepForServerRequest != null &&
                stateForServerRequest.shouldUseServerRequest() &&
                !stateForServerRequest.selectedObjects.containsKey(currentStepForServerRequest.id)) {

                Timber.d("Automatically requesting server object for step ${currentStepForServerRequest.id}")
                delay(300) // Небольшая задержка для UI
                requestServerObject()
            }
        }
    }

    private fun loadClassifierProductInfo(productId: String) {
        if (uiState.value.classifierProductInfo?.id == productId) {
            return  // Информация о товаре уже загружена, выходим из метода
        }

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
                            productInfoError = "Product $productId not found in database"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoadingProductInfo = false,
                        productInfoError = "Error loading product data: ${e.message}"
                    )
                }
            }
        }
    }

    fun confirmCurrentStep() {
        launchIO {
            val currentState = uiState.value

            // Если текущий шаг не виден, автоматически переходим к следующему видимому
            val currentStep = currentState.getCurrentStep()
            if (currentStep != null && !expressionEvaluator.evaluateVisibilityCondition(
                    currentStep.visibilityCondition,
                    currentState
                )) {
                Timber.d("Current step ${currentStep.id} is invisible, looking for next visible")

                val nextVisibleIndex = expressionEvaluator.findNextVisibleStepIndex(currentState)

                launchMain {
                    if (nextVisibleIndex != null) {
                        updateState { it.copy(currentStepIndex = nextVisibleIndex) }
                        initializeStepWithFiltersAndBuffer(nextVisibleIndex)
                    } else {
                        // Если нет видимых шагов впереди, переходим к экрану итогов
                        updateState { it.copy(showSummary = true) }
                    }
                }
                return@launchIO
            }

            // Выполняем валидацию в IO потоке
            val isValid = validateCurrentStep()

            if (isValid) {
                // Стандартная логика подтверждения шага в Main потоке
                launchMain {
                    val result = controller.confirmCurrentStep(uiState.value) {
                        // Валидация уже выполнена выше и прошла успешно
                        isValid
                    }

                    val newState = result.getNewState()
                    updateState { newState }

                    // Проверяем, показывается ли итоговый экран и нужно ли автоматически завершать действие
                    if (newState.showSummary) {
                        if (shouldAutoComplete(newState)) {
                            Timber.d("Automatic action completion after last step")
                            completeAction()
                        }
                    } else {
                        // После перехода инициализируем новый шаг
                        initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                    }
                }
            }
            // Если валидация не прошла, ошибка уже установлена в validateCurrentStep
        }
    }

    /**
     * Проверяет, должно ли действие быть завершено автоматически
     */
    private fun shouldAutoComplete(state: ActionWizardState): Boolean {
        val plannedAction = state.plannedAction ?: return false
        val actionTemplate = plannedAction.actionTemplate ?: return false
        return actionTemplate.isAutoCompleteEnabled()
    }

    fun previousStep() {
        val currentState = uiState.value

        // Если текущий шаг не виден, автоматически переходим к предыдущему видимому
        val currentStep = currentState.getCurrentStep()
        if (currentStep != null && !expressionEvaluator.evaluateVisibilityCondition(
                currentStep.visibilityCondition,
                currentState
            )) {
            Timber.d("Current step ${currentStep.id} is invisible, looking for previous visible")

            val prevVisibleIndex = expressionEvaluator.findPreviousVisibleStepIndex(currentState)
            if (prevVisibleIndex != null) {
                updateState { it.copy(currentStepIndex = prevVisibleIndex) }
                // Не инициализируем шаг при возврате назад
            } else {
                // Если нет видимых шагов позади, показываем диалог выхода
                updateState { it.copy(showExitDialog = true) }
            }
            return
        }

        // Стандартная логика возврата к предыдущему шагу
        val result = controller.previousStep(currentState)
        updateState { result.getNewState() }
    }

    fun setObjectForCurrentStep(obj: Any, autoAdvance: Boolean = true, skipValidation: Boolean = false) {
        // Отменяем все предыдущие jobs для предотвращения конкурирующих операций
        fieldUpdateJob?.cancel()
        
        if (obj is TaskProduct) {
            loadClassifierProductInfo(obj.product.id)
        } else if (obj is Product) {
            loadClassifierProductInfo(obj.id)
        }

        // Сначала устанавливаем объект как обычно
        val result = controller.setObjectForCurrentStep(uiState.value, obj)
        val newState = result.getNewState()
        updateState { newState }

        if (autoAdvance) {
            Timber.d("Initiating auto-advance after setting object: $obj, skipValidation=$skipValidation")
            
            // Запускаем автопереход в отдельной корутине с ожиданием обновления состояния
            launchIO {
                // Ждем, пока объект действительно появится в состоянии И в factAction
                val currentStep = newState.getCurrentStep()
                if (currentStep != null) {
                    var retries = 0
                    while (retries < 10) {
                        val currentState = uiState.value
                        val hasObjectInState = currentState.selectedObjects.containsKey(currentStep.id)
                        
                        // Проверяем, что объект также обновлен в factAction
                        val hasObjectInFactAction = when (currentStep.factActionField) {
                            FactActionField.STORAGE_PRODUCT -> currentState.factAction?.storageProduct != null
                            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> currentState.factAction?.storageProductClassifier != null
                            FactActionField.STORAGE_BIN -> currentState.factAction?.storageBin != null
                            FactActionField.STORAGE_PALLET -> currentState.factAction?.storagePallet != null
                            FactActionField.ALLOCATION_BIN -> currentState.factAction?.placementBin != null
                            FactActionField.ALLOCATION_PALLET -> currentState.factAction?.placementPallet != null
                            FactActionField.QUANTITY -> currentState.factAction?.quantity ?: 0f > 0f
                            else -> true
                        }
                        
                        if (hasObjectInState && hasObjectInFactAction) {
                            // Объект установлен и в состоянии, и в factAction
                            Timber.d("Object found in state and factAction after $retries retries")
                            break
                        }
                        
                        delay(50) // Ждем 50мс перед следующей проверкой
                        retries++
                    }
                    
                    if (retries >= 10) {
                        Timber.w("Object not fully updated after 10 retries, proceeding anyway")
                    }
                }
                
                // Теперь запускаем автопереход
                if (skipValidation) {
                    tryAutoAdvanceWithFieldUpdatesSkipValidation(obj)
                } else {
                    tryAutoAdvanceWithFieldUpdates(obj)
                }
            }
        }
    }

    private fun tryAutoAdvanceWithFieldUpdates(originalObject: Any) {
        // Отменяем предыдущий запрос на обновление полей
        fieldUpdateJob?.cancel()
        
        fieldUpdateJob = launchIO {
            val currentState = uiState.value
            val currentStep = currentState.getCurrentStep()

            // Сначала выполняем валидацию
            val isValid = validateCurrentStep()
            
            if (isValid) {
                // Если валидация прошла успешно и есть updateActionFieldEndpoint
                if (currentStep != null && currentStep.updateActionFieldEndpoint.isNotEmpty()) {
                    // Проверяем, что объект действительно установлен в состоянии
                    val selectedObject = currentState.selectedObjects[currentStep.id]
                    if (selectedObject == null) {
                        Timber.w("Object not found in state for step ${currentStep.id}, using original object")
                    }
                    
                    // Используем объект из состояния, если он есть, иначе используем оригинальный
                    val objectToSend = selectedObject ?: originalObject
                    
                    Timber.d("Validation passed, requesting field updates from server")
                    
                    updateState { it.copy(isRequestingServerObject = true) }
                    
                    val updateResult = networkService.getFieldUpdates(
                        endpoint = currentStep.updateActionFieldEndpoint,
                        factAction = currentState.factAction!!,
                        selectedObject = objectToSend,
                        fieldType = currentStep.factActionField
                    )
                    
                    updateState { it.copy(isRequestingServerObject = false) }
                    
                    if (updateResult.isSuccess()) {
                        val updatedObject = updateResult.getResponseData()
                        Timber.d("Field updates received, updating object in FactAction")
                        
                        launchMain {
                            // Обновляем только объект в состоянии без повторной валидации
                            val currentState = uiState.value
                            val updatedObjects = currentState.selectedObjects.toMutableMap()
                            updatedObjects[currentStep.id] = updatedObject!!
                            
                            // Обновляем FactAction через StateMachine
                            val tempStateForUpdate = currentState.copy(selectedObjects = updatedObjects)
                            val finalState = stateMachine.transition(tempStateForUpdate, WizardEvent.SetObject(updatedObject, currentStep.id))
                            updateState { finalState }
                            
                            // Продолжаем auto-advance без повторной валидации (валидация уже прошла)
                            tryAutoAdvanceSkipValidation()
                        }
                    } else {
                        Timber.e("Failed to get field updates: ${updateResult.getErrorMessage()}")
                        sendEvent(ActionWizardEvent.ShowSnackbar("Failed to get field updates: ${updateResult.getErrorMessage()}"))
                        
                        launchMain {
                            // В случае ошибки продолжаем с исходным объектом (валидация уже прошла)
                            tryAutoAdvanceSkipValidation()
                        }
                    }
                } else {
                    // Если updateActionFieldEndpoint не задан, продолжаем обычный auto-advance
                    launchMain {
                        tryAutoAdvance()
                    }
                }
            }
            // Если валидация не прошла, auto-advance не выполняется
        }
    }

    private fun tryAutoAdvance() {
        launchIO {
            val currentState = uiState.value

            // Если текущий шаг не виден, автоматически переходим к следующему видимому
            val currentStep = currentState.getCurrentStep()
            if (currentStep != null && !expressionEvaluator.evaluateVisibilityCondition(
                    currentStep.visibilityCondition,
                    currentState
                )) {
                Timber.d("Current step ${currentStep.id} is invisible, looking for next visible step for auto-advance")

                val nextVisibleIndex = expressionEvaluator.findNextVisibleStepIndex(currentState)

                launchMain {
                    if (nextVisibleIndex != null) {
                        updateState { it.copy(currentStepIndex = nextVisibleIndex) }
                        initializeStepWithFiltersAndBuffer(nextVisibleIndex)
                    } else {
                        // Если нет видимых шагов впереди, переходим к экрану итогов
                        updateState { it.copy(showSummary = true) }
                    }
                }
                return@launchIO
            }

            // Выполняем валидацию в IO потоке
            val isValid = validateCurrentStep()

            if (isValid) {
                // Переходим в Main поток для обновления состояния
                launchMain {
                    val result = controller.tryAutoAdvance(uiState.value) {
                        // Валидация уже выполнена выше
                        true
                    }

                    if (result.isSuccess()) {
                        val newState = result.getNewState()
                        updateState { newState }

                        // Проверяем, произошел ли переход на итоговый экран
                        if (newState.showSummary) {
                            if (shouldAutoComplete(newState)) {
                                Timber.d("Automatic action completion after auto-advance to summary screen")
                                completeAction()
                            }
                        } else {
                            // После автоперехода инициализируем новый шаг (только если не итоговый экран)
                            initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                        }
                    }
                }
            }
            // Если валидация не прошла, ошибка уже установлена в validateCurrentStep
        }
    }

    private fun tryAutoAdvanceWithFieldUpdatesSkipValidation(originalObject: Any) {
        // Отменяем предыдущий запрос на обновление полей
        fieldUpdateJob?.cancel()
        
        fieldUpdateJob = launchIO {
            val currentState = uiState.value
            val currentStep = currentState.getCurrentStep()

            // Пропускаем валидацию, так как она уже была выполнена в FieldHandler
            Timber.d("Skipping validation - object already validated in FieldHandler")
            
            // Если есть updateActionFieldEndpoint, запрашиваем обновления полей
            if (currentStep != null && currentStep.updateActionFieldEndpoint.isNotEmpty()) {
                // Проверяем, что объект действительно установлен в состоянии
                val selectedObject = currentState.selectedObjects[currentStep.id]
                if (selectedObject == null) {
                    Timber.w("Object not found in state for step ${currentStep.id}, using original object")
                }
                
                // Используем объект из состояния, если он есть, иначе используем оригинальный
                val objectToSend = selectedObject ?: originalObject
                
                Timber.d("Requesting field updates from server (validation skipped)")
                
                updateState { it.copy(isRequestingServerObject = true) }
                
                val updateResult = networkService.getFieldUpdates(
                    endpoint = currentStep.updateActionFieldEndpoint,
                    factAction = currentState.factAction!!,
                    selectedObject = objectToSend,
                    fieldType = currentStep.factActionField
                )
                
                updateState { it.copy(isRequestingServerObject = false) }
                
                if (updateResult.isSuccess()) {
                    val updatedObject = updateResult.getResponseData()
                    Timber.d("Field updates received, updating object in FactAction")
                    
                    launchMain {
                        // Обновляем только объект в состоянии без повторной валидации
                        val currentState = uiState.value
                        val updatedObjects = currentState.selectedObjects.toMutableMap()
                        updatedObjects[currentStep.id] = updatedObject!!
                        
                        // Обновляем FactAction через StateMachine
                        val tempStateForUpdate = currentState.copy(selectedObjects = updatedObjects)
                        val finalState = stateMachine.transition(tempStateForUpdate, WizardEvent.SetObject(updatedObject, currentStep.id))
                        updateState { finalState }
                        
                        // Продолжаем auto-advance без повторной валидации
                        tryAutoAdvanceSkipValidation()
                    }
                } else {
                    Timber.e("Failed to get field updates: ${updateResult.getErrorMessage()}")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Failed to get field updates: ${updateResult.getErrorMessage()}"))
                    
                    launchMain {
                        // В случае ошибки продолжаем с исходным объектом (валидация была пропущена)
                        tryAutoAdvanceSkipValidation()
                    }
                }
            } else {
                // Если updateActionFieldEndpoint не задан, продолжаем обычный auto-advance без валидации
                launchMain {
                    tryAutoAdvanceSkipValidation()
                }
            }
        }
    }

    private fun tryAutoAdvanceSkipValidation() {
        launchIO {
            val currentState = uiState.value

            // Если текущий шаг не виден, автоматически переходим к следующему видимому
            val currentStep = currentState.getCurrentStep()
            if (currentStep != null && !expressionEvaluator.evaluateVisibilityCondition(
                    currentStep.visibilityCondition,
                    currentState
                )) {
                Timber.d("Current step ${currentStep.id} is invisible, looking for next visible step for auto-advance")

                val nextVisibleIndex = expressionEvaluator.findNextVisibleStepIndex(currentState)

                launchMain {
                    if (nextVisibleIndex != null) {
                        updateState { it.copy(currentStepIndex = nextVisibleIndex) }
                        initializeStepWithFiltersAndBuffer(nextVisibleIndex)
                    } else {
                        // Если нет видимых шагов впереди, переходим к экрану итогов
                        updateState { it.copy(showSummary = true) }
                    }
                }
                return@launchIO
            }

            // Пропускаем валидацию, так как она уже была выполнена
            Timber.d("Skipping validation - already validated in tryAutoAdvanceWithFieldUpdates")
            
            // Переходим в Main поток для обновления состояния
            launchMain {
                val result = controller.tryAutoAdvance(uiState.value) {
                    // Валидация пропущена, так как уже была выполнена
                    true
                }

                if (result.isSuccess()) {
                    val newState = result.getNewState()
                    updateState { newState }

                    // Проверяем, произошел ли переход на итоговый экран
                    if (newState.showSummary) {
                        if (shouldAutoComplete(newState)) {
                            Timber.d("Automatic action completion after auto-advance to summary screen")
                            completeAction()
                        }
                    } else {
                        // После автоперехода инициализируем новый шаг (только если не итоговый экран)
                        initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                    }
                }
            }
            // Если валидация была не нужна, продолжаем
        }
    }

    /**
     * Автоматический переход для объектов из буфера с режимом ALWAYS
     */
    private fun tryAutoAdvanceFromBuffer() {
        launchIO {
            val currentState = uiState.value

            launchMain {
                val result = controller.tryAutoAdvanceFromBuffer(currentState)

                if (result.isSuccess()) {
                    val newState = result.getNewState()
                    updateState { newState }

                    // Проверяем, произошел ли переход на итоговый экран
                    if (newState.showSummary) {
                        if (shouldAutoComplete(newState)) {
                            Timber.d("Automatic action completion after auto-advance from buffer to summary screen")
                            completeAction()
                        }
                    } else {
                        // Рекурсивно проверяем следующий шаг на наличие значений в буфере или фильтрах
                        initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                    }
                }
            }
        }
    }

    private suspend fun validateCurrentStep(): Boolean {
        // Защита от множественных одновременных валидаций
        if (isValidating) {
            Timber.d("Validation already in progress, skipping")
            return false
        }

        isValidating = true

        try {
            val currentState = uiState.value

            // Используем новый метод валидации, который возвращает детальную информацию
            val validationResult = validator.validateCurrentStep(currentState)

            if (!validationResult.isValid) {
                val errorMessage = validationResult.errorMessage ?: "Validation failed"

                // ВАЖНО: Обновляем UI состояние в Main потоке
                launchMain {
                    // Устанавливаем детальную ошибку в состояние для отображения вверху экрана
                    updateState { it.copy(error = errorMessage) }

                    // И также отправляем в снекбар
                    sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                }
            }

            return validationResult.isValid
        } finally {
            isValidating = false
        }
    }

    fun handleBarcode(barcode: String) {
        // Проверка состояния экрана - не обрабатываем штрихкоды во время загрузки,
        // показа диалогов или на экране сводки
        if (uiState.value.isLoading ||
            uiState.value.showExitDialog ||
            uiState.value.showSummary) {
            Timber.d("Scanning ignored due to screen state")
            return
        }

        // Если текущий шаг заблокирован буфером, не обрабатываем сканирования
        if (uiState.value.isCurrentStepLockedByBuffer()) {
            Timber.d("Step is locked by buffer (ALWAYS mode), scanning ignored")
            sendEvent(ActionWizardEvent.ShowSnackbar("Field is locked by buffer"))
            return
        }

        // Если для шага используется serverSelectionEndpoint, не обрабатываем сканирования
        if (uiState.value.shouldUseServerRequest()) {
            Timber.d("Step uses serverSelectionEndpoint, scanning ignored")
            sendEvent(ActionWizardEvent.ShowSnackbar("For this step objects are received from the server"))
            return
        }

        // Показываем индикатор загрузки в UI потоке
        updateState { it.copy(isLoading = true) }

        // Выполняем валидацию в IO потоке
        launchIO {
            try {
                val result = objectSearchService.handleBarcode(uiState.value, barcode)

                // ВАЖНО: Обновляем UI состояние в Main потоке
                launchMain {
                    // Убираем индикатор загрузки
                    updateState { it.copy(isLoading = false) }

                    if (result.isSuccess() && result.isProcessingRequired()) {
                        val foundObject = result.getResultData()
                        if (foundObject != null) {
                            setObjectForCurrentStep(foundObject, true, true)
                        }
                    } else if (!result.isSuccess()) {
                        val errorMessage = result.getErrorMessage()
                        if (errorMessage != null) {
                            // Устанавливаем ошибку в состояние в Main потоке
                            updateState { it.copy(error = errorMessage) }
                            sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing barcode: $barcode")

                // ВАЖНО: Обработка ошибок также в Main потоке
                launchMain {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error processing barcode: ${e.message}"
                        )
                    }
                    sendEvent(ActionWizardEvent.ShowSnackbar("Error processing barcode: ${e.message}"))
                }
            }
        }
    }

    /**
     * Запрашивает объект с сервера для текущего шага
     */
    fun requestServerObject() {
        val currentState = uiState.value
        val currentStep = currentState.getCurrentStep() ?: return

        if (currentStep.serverSelectionEndpoint.isEmpty()) {
            sendEvent(ActionWizardEvent.ShowSnackbar("Endpoint not configured for this step"))
            return
        }

        // Если шаг заблокирован буфером, не запрашиваем объект
        if (currentState.isCurrentStepLockedByBuffer()) {
            Timber.d("Step is locked by buffer (ALWAYS mode), object request ignored")
            sendEvent(ActionWizardEvent.ShowSnackbar("Field is locked by buffer"))
            return
        }

        val factAction = currentState.factAction ?: return
        val cancellationToken = UUID.randomUUID().toString()

        updateState {
            it.copy(
                isRequestingServerObject = true,
                serverRequestCancellationToken = cancellationToken,
                error = null
            )
        }

        // Отменяем предыдущий запрос, если он был
        serverRequestJob?.cancel()

        serverRequestJob = launchIO {
            try {
                Timber.d("Requesting object from server: ${currentStep.serverSelectionEndpoint}")

                // Проверяем, не отменен ли запрос
                if (uiState.value.serverRequestCancellationToken != cancellationToken) {
                    Timber.d("Request canceled: token changed")
                    return@launchIO
                }

                val result = networkService.getStepObject(
                    endpoint = currentStep.serverSelectionEndpoint,
                    factAction = factAction,
                    fieldType = currentStep.factActionField
                )

                // Проверяем, не отменен ли запрос
                if (uiState.value.serverRequestCancellationToken != cancellationToken) {
                    Timber.d("Request canceled after receiving result: token changed")
                    return@launchIO
                }

                // Сбрасываем флаг запроса перед обработкой результата
                updateState {
                    it.copy(
                        isRequestingServerObject = false,
                        serverRequestCancellationToken = null
                    )
                }

                if (result.isSuccess()) {
                    val serverObject = result.getResponseData()
                    if (serverObject != null) {
                        Timber.d("Object successfully received from server: ${serverObject.javaClass.simpleName}")

                        // Устанавливаем полученный объект в текущий шаг
                        val stateResult = controller.handleServerObjectRequest(uiState.value, result)
                        updateState { stateResult.getNewState() }

                        // Если объект успешно установлен и нет ошибок, показываем уведомление
                        if (stateResult.isSuccess() && stateResult.getNewState().error == null) {
                            sendEvent(ActionWizardEvent.ShowSnackbar("Object successfully received from server"))
                        }
                    } else {
                        Timber.w("Server returned successful response, but object was not received")
                        updateState {
                            it.copy(error = "Failed to get object from server")
                        }
                        sendEvent(ActionWizardEvent.ShowSnackbar("Failed to get object from server"))
                    }
                } else {
                    Timber.w("Error requesting object from server: ${result.getErrorMessage()}")
                    updateState {
                        it.copy(error = result.getErrorMessage())
                    }
                    sendEvent(ActionWizardEvent.ShowSnackbar(
                        result.getErrorMessage() ?: "Error getting object from server"
                    ))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception when requesting object from server")

                updateState {
                    it.copy(
                        isRequestingServerObject = false,
                        serverRequestCancellationToken = null,
                        error = "Error: ${e.message}"
                    )
                }

                sendEvent(ActionWizardEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    /**
     * Отменяет текущий запрос объекта с сервера
     */
    fun cancelServerRequest() {
        launchIO {
            try {
                serverRequestJob?.cancelAndJoin()
            } catch (e: Exception) {
                Timber.e(e, "Error canceling server object request")
            } finally {
                serverRequestJob = null

                updateState {
                    it.copy(
                        isRequestingServerObject = false,
                        serverRequestCancellationToken = null
                    )
                }
            }
        }
    }

    fun completeAction() {
        launchIO {
            val currentState = uiState.value
            val factAction = currentState.factAction ?: return@launchIO
            val plannedAction = currentState.plannedAction ?: return@launchIO

            if (!validator.canComplete(currentState)) {
                sendEvent(ActionWizardEvent.ShowSnackbar("Not all required steps are completed"))
                return@launchIO
            }

            try {
                updateState { controller.submitForm(it).getNewState() }

                // Проверяем необходимость автозаполнения полей из плана
                val actionTemplate = plannedAction.actionTemplate
                val shouldAutoFill = actionTemplate?.autoFillFromPlan == true

                // Дозаполняем фактическое действие данными из планового, если нужно
                val finalFactAction = if (shouldAutoFill) {
                    Timber.d("Дозаполнение полей фактического действия из плана")

                    // Простое дозаполнение незаполненных полей из плана
                    factAction.copy(
                        storageBin = factAction.storageBin ?: plannedAction.storageBin,
                        storagePallet = factAction.storagePallet ?: plannedAction.storagePallet,
                        placementBin = factAction.placementBin ?: plannedAction.placementBin,
                        placementPallet = factAction.placementPallet ?: plannedAction.placementPallet,
                        storageProduct = factAction.storageProduct ?: plannedAction.storageProduct,
                        storageProductClassifier = factAction.storageProductClassifier ?: plannedAction.storageProductClassifier,
                        quantity = if (factAction.quantity <= 0f) plannedAction.quantity else factAction.quantity
                    )
                } else {
                    factAction
                }

                val syncWithServer = actionTemplate?.syncWithServer == true
                val result = networkService.completeAction(finalFactAction, syncWithServer)

                if (result.isSuccess()) {
                    updateState { controller.handleSendSuccess(it).getNewState() }

                    // Вызываем callback из главного потока с задержкой для завершения обработки
                    launchMain {
                        navigateBackCallback?.invoke()
                    }
                } else {
                    Timber.d("ActionWizardViewModel: ошибка отправки: ${result.getErrorMessage()}")
                    // Явно сбрасываем состояние загрузки перед установкой ошибки
                    updateState { it.copy(isLoading = false) }
                    updateState {
                        controller.handleSendFailure(
                            it,
                            result.getErrorMessage() ?: "Unknown error"
                        ).getNewState()
                    }
                    sendEvent(
                        ActionWizardEvent.ShowSnackbar(
                            result.getErrorMessage() ?: "Failed to send data"
                        )
                    )
                }
            } catch (e: Exception) {
                // Добавляем обработку исключений с явным сбросом флага загрузки
                Timber.e(e, "Error sending data: ${e.message}")
                updateState { it.copy(isLoading = false, sendingFailed = true, error = e.message) }
                sendEvent(ActionWizardEvent.ShowSnackbar("Error: ${e.message}"))
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
        Timber.d("ActionWizardViewModel: вызов метода exitWizard()")
        clearErrorAndLoading()
        updateState { controller.dismissExitDialog(it).getNewState() }

        // Используем callback вместо отправки события
        launchMain {
            navigateBackCallback?.invoke()
            Timber.d("ActionWizardViewModel: callback навигации вызван из exitWizard()")
        }
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

    fun executeStepCommand(command: StepCommand, parameters: Map<String, String>) {
        val currentState = uiState.value
        val currentStep = currentState.getCurrentStep() ?: return
        val factAction = currentState.factAction ?: return

        // Предотвращаем множественное выполнение одной команды
        if (executingCommands.contains(command.id)) {
            Timber.d("Command ${command.id} is already executing, ignoring")
            return
        }

        executingCommands.add(command.id)

        updateState { it.copy(isLoading = true, error = null) }

        launchIO {
            try {
                Timber.d("Executing command: ${command.name} (${command.id})")

                val result = networkService.executeCommand(
                    command = command,
                    stepId = currentStep.id,
                    factAction = factAction,
                    parameters = parameters,
                    additionalContext = buildAdditionalContext(currentState)
                )

                // Убираем состояние загрузки
                updateState { it.copy(isLoading = false) }

                if (result.isSuccess()) {
                    val executionResult = result.getResponseData()
                    if (executionResult != null) {
                        updateCommandStatus(command, true, executionResult.message)

                        handleCommandExecutionResult(executionResult)
                    }
                } else {
                    val errorMessage = result.getErrorMessage()?.takeIf { it.isNotBlank() } ?: "Command execution error"

                    updateCommandStatus(command, false, errorMessage)

                    updateState { it.copy(error = errorMessage) }
                    sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while executing command ${command.id}")

                updateCommandStatus(command, false, e.message)

                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
                val exceptionMessage = e.message
                if (!exceptionMessage.isNullOrBlank()) {
                    sendEvent(ActionWizardEvent.ShowSnackbar("Error: $exceptionMessage"))
                } else {
                    sendEvent(ActionWizardEvent.ShowSnackbar("Command execution failed"))
                }
            } finally {
                executingCommands.remove(command.id)
            }
        }
    }

    /**
     * Строит дополнительный контекст для команды
     */
    private fun buildAdditionalContext(state: ActionWizardState): Map<String, String> {
        val context = mutableMapOf<String, String>()

        // Добавляем информацию о текущем состоянии
        context["currentStepIndex"] = state.currentStepIndex.toString()
        context["totalSteps"] = state.steps.size.toString()

        // Добавляем информацию о выбранных объектах
        state.selectedObjects.forEach { (stepId, obj) ->
            context["selectedObject_$stepId"] = obj.toString()
        }

        // Добавляем информацию о плановом действии
        state.plannedAction?.let { plannedAction ->
            context["plannedActionId"] = plannedAction.id
            context["plannedQuantity"] = plannedAction.quantity.toString()
        }

        return context
    }

    /**
     * Обрабатывает результат выполнения команды
     */
    private fun handleCommandExecutionResult(result: CommandExecutionResult) {
        Timber.d("Processing command result: success=${result.success}")

        // 1. Обновляем factAction, если сервер вернул обновленные данные
        result.updatedFactAction?.let { updatedFactActionDto ->
            val currentFactAction = uiState.value.factAction
            if (currentFactAction != null) {
                // Используем метод из networkService для преобразования DTO в доменную модель
                val updatedDomainFactAction = networkService.mapDtoToFactAction(
                    updatedFactActionDto,
                    currentFactAction
                )

                if (updatedDomainFactAction != null) {
                    updateState { state ->
                        state.copy(factAction = updatedDomainFactAction)
                    }
                }
            }
        }

        // 2. Обрабатываем дополнительные данные результата
        if (result.resultData.isNotEmpty()) {
            // Сохраняем данные результата для возможного использования в UI
            updateState { state ->
                state.copy(lastCommandResultData = result.resultData)
            }
        }

        // 3. Определяем следующее действие на основе nextAction или поведения команды
        val actionToPerform = result.nextAction ?: when (result.commandBehavior) {
            CommandExecutionBehavior.SHOW_RESULT -> CommandNextAction.SHOW_DIALOG
            CommandExecutionBehavior.REFRESH_STEP -> CommandNextAction.REFRESH_STEP
            CommandExecutionBehavior.GO_TO_NEXT_STEP -> CommandNextAction.GO_TO_NEXT_STEP
            CommandExecutionBehavior.GO_TO_PREVIOUS_STEP -> CommandNextAction.GO_TO_PREVIOUS_STEP
            CommandExecutionBehavior.COMPLETE_ACTION -> CommandNextAction.COMPLETE_ACTION
            CommandExecutionBehavior.SILENT -> CommandNextAction.NONE
        }

        // 4. Выполняем действие в зависимости от actionToPerform
        when (actionToPerform) {
            CommandNextAction.NONE -> {
                // Ничего не делаем
            }
            CommandNextAction.REFRESH_STEP -> {
                // Обновляем текущий шаг
                initializeStepWithFiltersAndBuffer(uiState.value.currentStepIndex)
            }
            CommandNextAction.GO_TO_NEXT_STEP -> {
                confirmCurrentStep()
            }
            CommandNextAction.GO_TO_PREVIOUS_STEP -> {
                previousStep()
            }
            CommandNextAction.COMPLETE_ACTION -> {
                completeAction()
            }
            CommandNextAction.SET_OBJECT -> {
                // Обрабатываем установку объекта из resultData
                handleSetObjectFromCommand(result.resultData)
            }
            CommandNextAction.SHOW_DIALOG -> {
                // Показываем подробный диалог с результатом выполнения
                showResultDialog(result)
                if (!result.message.isNullOrBlank()) {
                    sendEvent(ActionWizardEvent.ShowSnackbar(result.message))
                }
            }
        }
    }

    /**
     * Обрабатывает установку объекта из результата команды
     */
    private fun handleSetObjectFromCommand(resultData: Map<String, String>) {
        val currentStep = uiState.value.getCurrentStep() ?: return

        launchIO {
            try {
                val obj = networkService.createObjectFromResultData(
                    resultData = resultData,
                    fieldType = currentStep.factActionField
                )

                // Если объект создан, устанавливаем его в текущий шаг
                if (obj != null) {
                    launchMain {
                        setObjectForCurrentStep(obj, true)
                    }
                } else {
                    Timber.w("Failed to create object from command result for field ${currentStep.factActionField}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing object from command result")
            }
        }
    }

    fun dismissResultDialog() {
        updateState { it.copy(showResultDialog = false) }
    }

    private fun showResultDialog(result: CommandExecutionResult) {
        val dialogTitle = result.message ?: "Command execution result"
        val dialogContent = prepareResultContent(result.resultData)

        updateState { state ->
            state.copy(
                showResultDialog = true,
                resultDialogTitle = dialogTitle,
                resultDialogContent = dialogContent
            )
        }
    }

    private fun prepareResultContent(resultData: Map<String, String>): List<Pair<String, String>> {
        return resultData.entries
            .filter { it.key != "dialogMessage" } // Исключаем служебные поля
            .map {
                // Преобразуем camelCase ключи в человекочитаемый формат
                val displayName = it.key
                    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                    .replaceFirstChar { char -> char.uppercase() }

                Pair(displayName, it.value)
            }
            .sortedBy { it.first }
    }

    private fun updateCommandStatus(command: StepCommand, success: Boolean, message: String? = null) {
        val currentStep = uiState.value.getCurrentStep() ?: return

        updateState { state ->
            val updatedCommands = state.executedCommands.toMutableMap()
            updatedCommands[command.id] = CommandExecutionStatus(
                commandId = command.id,
                stepId = currentStep.id,
                success = success,
                timestamp = System.currentTimeMillis(),
                message = message
            )
            state.copy(executedCommands = updatedCommands)
        }
    }

    override fun onCleared() {
        fieldUpdateJob?.cancel()
        serverRequestJob?.cancel()
        resetProcessingJob?.cancel()
        super.onCleared()
    }
}