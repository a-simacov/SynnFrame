package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.data.remote.dto.CommandNextAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.entity.CommandExecutionBehavior
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.wizard.handler.FieldHandlerFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.CommandExecutionResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.ObjectSearchService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardController
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardNetworkService
import com.synngate.synnframe.presentation.ui.taskx.wizard.service.WizardValidator
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.state.WizardStateMachine
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    taskXRepository: TaskXRepository,
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

    private val stateMachine = WizardStateMachine()
    private val validator = WizardValidator(validationService, handlerFactory)
    private val controller = WizardController(stateMachine)
    private val objectSearchService = ObjectSearchService(productUseCases, validationService)

    // Добавить новое состояние для команд
    private var executingCommands = mutableSetOf<String>()

    private var isProcessingBarcode = false
    private var resetProcessingJob: Job? = null

    // Новый Job для запроса объекта с сервера
    private var serverRequestJob: Job? = null

    // Новый callback для навигации
    private var navigateBackCallback: (() -> Unit)? = null

    // Метод для установки callback'а навигации
    fun setNavigateBackCallback(callback: () -> Unit) {
        navigateBackCallback = callback
        Timber.d("Установлен callback навигации назад из визарда")
    }

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

        // Проверяем не только буфер, но и активные фильтры
        initializeStepWithFiltersAndBuffer(initResult.getNewState().currentStepIndex)
    }

    /**
     * Инициализация шага: проверка и применение значений из буфера и фильтров
     */
    private fun initializeStepWithFiltersAndBuffer(stepIndex: Int) {
        val state = uiState.value
        if (stepIndex < 0 || stepIndex >= state.steps.size) return

        launchIO {
            // 1. Сначала проверяем и применяем значения из буфера
            val applyBufferResult = controller.applyBufferValueIfNeeded(state)
            updateState { applyBufferResult.getNewState() }

            // Проверяем, было ли изменено состояние после применения буфера
            val bufferApplied = applyBufferResult.getNewState() != state

            // 2. Затем проверяем и применяем значения из фильтров (если есть)
            val activeFilters = TaskXDataHolderSingleton.actionsFilter.getActiveFilters()

            // Получаем текущий шаг после применения буфера
            val currentStep = applyBufferResult.getNewState().getCurrentStep()

            if (currentStep != null && !bufferApplied) {
                // Ищем фильтр, соответствующий текущему шагу
                val matchingFilter = activeFilters.find { it.field == currentStep.factActionField }

                if (matchingFilter != null) {
                    Timber.d("Найден соответствующий фильтр для шага ${currentStep.id}: ${matchingFilter.field}")

                    // Устанавливаем значение из фильтра
                    setObjectForCurrentStep(matchingFilter.data, true)

                    // Для логирования
                    Timber.d("Значение из фильтра ${matchingFilter.field} установлено в шаг ${currentStep.id}")
                    return@launchIO
                }
            }

            // 3. Если объект из буфера успешно применен, выполняем автопереход для заблокированных полей
            if (bufferApplied && applyBufferResult.getNewState().isCurrentStepLockedByBuffer()) {
                delay(300) // Небольшая задержка для UI
                tryAutoAdvanceFromBuffer()
            }

            // 4. Если для шага настроен serverSelectionEndpoint и ни буфер, ни фильтр не применились,
            // автоматически запрашиваем объект с сервера
            val updatedState = uiState.value
            if (updatedState.shouldUseServerRequest() &&
                !updatedState.selectedObjects.containsKey(updatedState.getCurrentStep()?.id ?: "")) {
                delay(300) // Небольшая задержка для UI
                requestServerObject()
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
            val currentState = uiState.value
            val result = controller.confirmCurrentStep(currentState) {
                validateCurrentStep()
            }

            val newState = result.getNewState()
            updateState { newState }

            // Проверяем, показывается ли итоговый экран и нужно ли автоматически завершать действие
            if (newState.showSummary) {
                if (shouldAutoComplete(newState)) {
                    Timber.d("Автоматическое завершение действия после последнего шага")
                    completeAction()
                }
            } else {
                // После перехода на новый шаг инициализируем его (только если не итоговый экран)
                initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
            }
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
                val newState = result.getNewState()
                updateState { newState }

                // Проверяем, произошел ли переход на итоговый экран
                if (newState.showSummary) {
                    if (shouldAutoComplete(newState)) {
                        Timber.d("Автоматическое завершение действия после автоперехода на итоговый экран")
                        completeAction()
                    }
                } else {
                    // После автоперехода инициализируем новый шаг (только если не итоговый экран)
                    initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                }
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
                val newState = result.getNewState()
                updateState { newState }

                // Проверяем, произошел ли переход на итоговый экран
                if (newState.showSummary) {
                    if (shouldAutoComplete(newState)) {
                        Timber.d("Автоматическое завершение действия после автоперехода из буфера на итоговый экран")
                        completeAction()
                    }
                } else {
                    // Рекурсивно проверяем следующий шаг на наличие значений в буфере или фильтрах
                    initializeStepWithFiltersAndBuffer(newState.currentStepIndex)
                }
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
        // Проверка состояния экрана - не обрабатываем штрихкоды во время загрузки,
        // показа диалогов или на экране сводки
        if (uiState.value.isLoading ||
            uiState.value.showExitDialog ||
            uiState.value.showSummary) {
            Timber.d("Сканирование игнорируется из-за состояния экрана")
            return
        }

        // Если текущий шаг заблокирован буфером, не обрабатываем сканирования
        if (uiState.value.isCurrentStepLockedByBuffer()) {
            Timber.d("Шаг заблокирован буфером (режим ALWAYS), сканирование игнорируется")
            sendEvent(ActionWizardEvent.ShowSnackbar("Поле заблокировано буфером"))
            return
        }

        // Если для шага используется serverSelectionEndpoint, не обрабатываем сканирования
        if (uiState.value.shouldUseServerRequest()) {
            Timber.d("Шаг использует serverSelectionEndpoint, сканирование игнорируется")
            sendEvent(ActionWizardEvent.ShowSnackbar("Для этого шага объекты получаются с сервера"))
            return
        }

        // Показываем индикатор загрузки
        updateState { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val result = objectSearchService.handleBarcode(uiState.value, barcode)

                // Убираем индикатор загрузки
                updateState { it.copy(isLoading = false) }

                if (result.isSuccess() && result.isProcessingRequired()) {
                    val foundObject = result.getResultData()
                    if (foundObject != null) {
                        setObjectForCurrentStep(foundObject, true)
                    }
                } else if (!result.isSuccess()) {
                    val errorMessage = result.getErrorMessage()
                    if (errorMessage != null) {
                        updateState { it.copy(error = errorMessage) }
                        sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
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
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка при обработке штрих-кода: ${e.message}"))
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
            sendEvent(ActionWizardEvent.ShowSnackbar("Endpoint не настроен для этого шага"))
            return
        }

        // Если шаг заблокирован буфером, не запрашиваем объект
        if (currentState.isCurrentStepLockedByBuffer()) {
            Timber.d("Шаг заблокирован буфером (режим ALWAYS), запрос объекта игнорируется")
            sendEvent(ActionWizardEvent.ShowSnackbar("Поле заблокировано буфером"))
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
                Timber.d("Запрос объекта с сервера: ${currentStep.serverSelectionEndpoint}")

                // Проверяем, не отменен ли запрос
                if (uiState.value.serverRequestCancellationToken != cancellationToken) {
                    Timber.d("Запрос отменен: токен изменился")
                    return@launchIO
                }

                val result = networkService.getStepObject(
                    endpoint = currentStep.serverSelectionEndpoint,
                    factAction = factAction,
                    fieldType = currentStep.factActionField
                )

                // Проверяем, не отменен ли запрос
                if (uiState.value.serverRequestCancellationToken != cancellationToken) {
                    Timber.d("Запрос отменен после получения результата: токен изменился")
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
                        Timber.d("Объект успешно получен с сервера: ${serverObject.javaClass.simpleName}")

                        // Устанавливаем полученный объект в текущий шаг
                        val stateResult = controller.handleServerObjectRequest(uiState.value, result)
                        updateState { stateResult.getNewState() }

                        // Если объект успешно установлен и нет ошибок, показываем уведомление
                        if (stateResult.isSuccess() && stateResult.getNewState().error == null) {
                            sendEvent(ActionWizardEvent.ShowSnackbar("Объект успешно получен с сервера"))
                        }
                    } else {
                        Timber.w("Сервер вернул успешный ответ, но объект не был получен")
                        updateState {
                            it.copy(error = "Не удалось получить объект с сервера")
                        }
                        sendEvent(ActionWizardEvent.ShowSnackbar("Не удалось получить объект с сервера"))
                    }
                } else {
                    Timber.w("Ошибка при запросе объекта с сервера: ${result.getErrorMessage()}")
                    updateState {
                        it.copy(error = result.getErrorMessage())
                    }
                    sendEvent(ActionWizardEvent.ShowSnackbar(
                        result.getErrorMessage() ?: "Ошибка при получении объекта с сервера"
                    ))
                }
            } catch (e: Exception) {
                Timber.e(e, "Исключение при запросе объекта с сервера")

                updateState {
                    it.copy(
                        isRequestingServerObject = false,
                        serverRequestCancellationToken = null,
                        error = "Ошибка: ${e.message}"
                    )
                }

                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
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
                Timber.e(e, "Ошибка при отмене запроса объекта с сервера")
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
        Timber.d("ActionWizardViewModel: начало метода completeAction()")
        launchIO {
            val currentState = uiState.value
            val factAction = currentState.factAction ?: return@launchIO
            val plannedAction = currentState.plannedAction ?: return@launchIO

            if (!validator.canComplete(currentState)) {
                sendEvent(ActionWizardEvent.ShowSnackbar("Не все обязательные шаги выполнены"))
                return@launchIO
            }

            try {
                Timber.d("ActionWizardViewModel: отправка данных на сервер")
                updateState { controller.submitForm(it).getNewState() }

                val syncWithServer = plannedAction.actionTemplate?.syncWithServer == true
                val result = networkService.completeAction(factAction, syncWithServer)

                if (result.isSuccess()) {
                    Timber.d("ActionWizardViewModel: успешная отправка, вызов навигации через callback")
                    updateState { controller.handleSendSuccess(it).getNewState() }

                    // Вызываем callback из главного потока с задержкой для завершения обработки
                    launchMain {
                        Timber.d("Вызов навигации из главного потока")
                        navigateBackCallback?.invoke()
                    }
                } else {
                    Timber.d("ActionWizardViewModel: ошибка отправки: ${result.getErrorMessage()}")
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
            Timber.d("Команда ${command.id} уже выполняется, игнорируем")
            return
        }

        executingCommands.add(command.id)

        updateState { it.copy(isLoading = true, error = null) }

        launchIO {
            try {
                Timber.d("Выполнение команды: ${command.name} (${command.id})")

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
                        handleCommandExecutionResult(executionResult)
                    }
                } else {
                    val errorMessage = result.getErrorMessage() ?: "Ошибка выполнения команды"
                    updateState { it.copy(error = errorMessage) }
                    sendEvent(ActionWizardEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Исключение при выполнении команды ${command.id}")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
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
        Timber.d("Обработка результата команды: success=${result.success}")

        // 1. Обновляем factAction, если сервер вернул обновленные данные
        result.updatedFactAction?.let { updatedFactAction ->
            updateState { state ->
                state.copy(factAction = updatedFactAction)
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
                if (result.message != null) {
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
                    Timber.w("Не удалось создать объект из результата команды для поля ${currentStep.factActionField}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке объекта из результата команды")
            }
        }
    }

    fun dismissResultDialog() {
        updateState { it.copy(showResultDialog = false) }
    }

    private fun showResultDialog(result: CommandExecutionResult) {
        val dialogTitle = result.message ?: "Результат выполнения команды"
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
}