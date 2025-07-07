package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.exception.TaskCompletionException
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter
import com.synngate.synnframe.presentation.ui.taskx.model.OperationResult
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import com.synngate.synnframe.presentation.ui.taskx.model.TaskCompletionResult
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.filter.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.service.ActionSearchService
import com.synngate.synnframe.presentation.ui.taskx.validator.ActionValidator
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

class TaskXDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val productUseCases: ProductUseCases,
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(
    initialState = TaskXDetailState(
        actionFilter = ActionFilter.CURRENT
    )
) {

    private val actionValidator = ActionValidator()
    private val actionSearchService = ActionSearchService(productUseCases)

    // Отслеживание объектов, добавленных в буфер из фильтра
    private val filterObjectsAddedToBuffer = mutableSetOf<FactActionField>()

    // Отслеживание последнего добавленного фильтра
    private var lastAddedFilterField: FactActionField? = null

    init {
        Timber.d("TaskXDetailViewModel init: taskId=$taskId, endpoint=$endpoint")
        Timber.d("Current cached task in singleton: ${TaskXDataHolderSingleton.currentTask.value?.id}")
        
        loadTask()
        loadCurrentUser()
        updateFilterState()
        updateBufferState()
        observeTaskUpdates()
    }

    private fun loadTask() {
        launchIO {
            // Очищаем данные предыдущего задания, если открываем новое
            val currentTaskId = TaskXDataHolderSingleton.currentTask.value?.id
            if (currentTaskId != null && currentTaskId != taskId) {
                Timber.d("Opening new task $taskId, clearing previous task data from $currentTaskId")
                TaskXDataHolderSingleton.forceClean()
            }
            
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val startEndpoint = "$endpoint/$taskId/take"
                Timber.d("Loading task $taskId from endpoint: $startEndpoint")
                val taskResult = dynamicMenuUseCases.startDynamicTask(startEndpoint, taskId)

                if (taskResult.isSuccess()) {
                    val task = taskResult.getOrNull()
                    if (task != null && task.taskType != null) {
                        TaskXDataHolderSingleton.setTaskData(task, task.taskType, endpoint)

                        val actionUiModels = createActionUiModels(task)

                        val searchEnabled = task.taskType.isActionSearchEnabled()

                        updateState {
                            it.copy(
                                task = task,
                                taskType = task.taskType,
                                actionUiModels = actionUiModels,
                                isLoading = false,
                                showSearchBar = searchEnabled
                            )
                        }
                    } else {
                        Timber.e("Task or its type not defined after loading")
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load task data"
                            )
                        }
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Error loading task data"))
                    }
                } else {
                    val error = (taskResult as? ApiResult.Error)?.message ?: "Unknown error"
                    Timber.e("Error loading task: $error")
                    updateState { it.copy(isLoading = false, error = error) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Loading error: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception when loading task $taskId")
                updateState { it.copy(isLoading = false, error = e.message) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    private fun createActionUiModels(task: TaskX): List<PlannedActionUI> {
        val isTaskInProgress = task.status == TaskXStatus.IN_PROGRESS

        return task.plannedActions.map { action ->
            PlannedActionUI.fromDomain(
                action = action,
                factActions = task.factActions,
                isTaskInProgress = isTaskInProgress
            )
        }
    }

    private fun loadCurrentUser() {
        launchIO {
            userUseCases.getCurrentUser().collect { user ->
                updateState { it.copy(currentUserId = user?.id) }
            }
        }
    }

    /**
     * Подписывается на обновления задачи из TaskXDataHolderSingleton
     */
    private fun observeTaskUpdates() {
        launchIO {
            TaskXDataHolderSingleton.currentTask.collect { updatedTask ->
                if (updatedTask != null && updatedTask.id == taskId) {
                    // Обновляем задачу и пересоздаем UI модели действий
                    val actionUiModels = createActionUiModels(updatedTask)
                    
                    updateState { state ->
                        state.copy(
                            task = updatedTask,
                            actionUiModels = actionUiModels,
                            isLoading = false
                        )
                    }
                    
                    Timber.d("Task updated from TaskXDataHolderSingleton: ${updatedTask.factActions.size} fact actions")
                }
            }
        }
    }

    fun onActionClick(actionId: String) {
        val task = uiState.value.task ?: return
        val actionUiModel = uiState.value.actionUiModels.find { it.id == actionId } ?: return

        if (!actionUiModel.isClickable) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Action already completed"))
            return
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Task must be in 'In Progress' status"))
            return
        }

        val validationResult = actionValidator.canExecuteAction(task, actionId)
        if (!validationResult.isSuccess) {
            showValidationError(validationResult.errorMessage ?: "Unable to execute action")
            return
        }

        if (!TaskXDataHolderSingleton.hasData()) {
            TaskXDataHolderSingleton.setTaskData(task, task.taskType!!, endpoint)

            if (!TaskXDataHolderSingleton.hasData()) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Error: cannot start wizard. Data unavailable."))
                return
            }
        }

        sendEvent(TaskXDetailEvent.NavigateToActionWizard(task.id, actionId))
    }

    private fun showValidationError(message: String) {
        updateState {
            it.copy(
                showValidationErrorDialog = true,
                validationErrorMessage = message
            )
        }
    }

    fun dismissValidationErrorDialog() {
        updateState {
            it.copy(
                showValidationErrorDialog = false,
                validationErrorMessage = null
            )
        }
    }

    fun onFilterChange(filter: ActionFilter) {
        updateState {
            it.copy(actionFilter = filter)
        }
    }

    fun onBackPressed() {
        val task = uiState.value.task

        if (task?.status == TaskXStatus.IN_PROGRESS || task?.status == TaskXStatus.PAUSED) {
            updateState { it.copy(showExitDialog = true) }
        } else {
            sendEvent(TaskXDetailEvent.NavigateBack)
        }
    }

    fun dismissExitDialog() {
        updateState { 
            it.copy(
                showExitDialog = false,
                exitDialogOperationResult = null
            )
        }
    }

    fun exitWithoutSaving() {
        dismissExitDialog()
        TaskXDataHolderSingleton.forceClean()
        sendEvent(TaskXDetailEvent.NavigateBack)
    }

    fun continueWork() {
        dismissExitDialog()
    }

    fun onExitDialogOkClick() {
        updateState { 
            it.copy(
                exitDialogOperationResult = null,
                showExitDialog = false
            )
        }
        // Навигация к списку заданий, т.к. OK показывается только при успешной операции
        sendEvent(TaskXDetailEvent.NavigateBack)
    }


    private fun handleExitDialogError(error: Throwable?, defaultMessage: String) {
        when (error) {
            is TaskCompletionException -> {
                val userMessage = error.userMessage
                if (!userMessage.isNullOrBlank()) {
                    if (error.isSuccess) {
                        // Успешное выполнение с userMessage
                        TaskXDataHolderSingleton.forceClean()
                    }
                    updateState {
                        it.copy(
                            exitDialogOperationResult = OperationResult.UserMessage(
                                message = userMessage,
                                isSuccess = error.isSuccess
                            )
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            exitDialogOperationResult = OperationResult.Error(
                                message = error.message ?: defaultMessage
                            )
                        )
                    }
                }
            }
            else -> {
                val errorMessage = error?.message ?: defaultMessage
                updateState {
                    it.copy(
                        exitDialogOperationResult = OperationResult.Error(errorMessage)
                    )
                }
            }
        }
    }

    fun pauseTask() {
        val task = uiState.value.task ?: return

        updateState {
            it.copy(
                isProcessingAction = true,
                exitDialogOperationResult = null
            )
        }

        launchIO {
            try {
                Timber.d("Pausing task...")
                val result = taskXUseCases.pauseTask(task.id, endpoint)
                
                updateState { it.copy(isProcessingAction = false) }

                if (result.isSuccess) {
                    // Задача успешно приостановлена без userMessage
                    result.getOrNull()?.let { updatedTask ->
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    TaskXDataHolderSingleton.forceClean()
                    sendEvent(TaskXDetailEvent.NavigateBack)
                } else {
                    val error = result.exceptionOrNull()
                    handleExitDialogError(error, "Error pausing task")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error pausing task: ${e.message}")
                updateState { it.copy(isProcessingAction = false) }
                handleExitDialogError(e, "Error pausing task")
            }
        }
    }

    fun completeTask() {
        val currentState = uiState.value
        val task = currentState.task
        val taskType = currentState.taskType
        
        Timber.d("completeTask called - task: ${task?.id}, taskType: ${taskType?.id}, isProcessing: ${currentState.isProcessingAction}")
        
        if (task == null) {
            Timber.w("completeTask: task is null")
            return
        }
        
        if (taskType == null) {
            Timber.w("completeTask: taskType is null")
            return
        }
        
        // Проверяем, не выполняется ли уже другая операция
        if (currentState.isProcessingAction) {
            Timber.w("completeTask: already processing action")
            return
        }

        val validationResult = actionValidator.canCompleteTask(task)
        if (!validationResult.isSuccess && !taskType.allowCompletionWithoutFactActions) {
            // Если вызов из диалога выхода, показываем ошибку в диалоге
            if (currentState.showExitDialog) {
                updateState {
                    it.copy(
                        exitDialogOperationResult = OperationResult.Error(
                            validationResult.errorMessage ?: "Unable to complete task"
                        )
                    )
                }
            } else {
                // Если вызов из диалога автозавершения, показываем ошибку в нем
                updateState {
                    it.copy(
                        taskCompletionResult = TaskCompletionResult(
                            message = validationResult.errorMessage ?: "Unable to complete task",
                            isSuccess = false
                        )
                    )
                }
            }
            return
        }

        // Определяем, откуда вызван метод
        if (currentState.showExitDialog) {
            processTaskCompletionFromExitDialog(task.id, endpoint)
        } else {
            processTaskCompletionFromCompletionDialog(task.id, endpoint)
        }
    }

    private fun processTaskCompletionFromExitDialog(taskId: String, endpoint: String) {
        updateState {
            it.copy(
                isProcessingAction = true,
                exitDialogOperationResult = null
            )
        }

        launchIO {
            try {
                Timber.d("Completing task from exit dialog...")
                val result = taskXUseCases.completeTask(taskId, endpoint)
                
                updateState { it.copy(isProcessingAction = false) }

                if (result.isSuccess) {
                    // Задача успешно завершена без userMessage
                    TaskXDataHolderSingleton.forceClean()
                    sendEvent(TaskXDetailEvent.NavigateBack)
                } else {
                    val error = result.exceptionOrNull()
                    handleExitDialogError(error, "Error completing task")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing task: ${e.message}")
                updateState { it.copy(isProcessingAction = false) }
                handleExitDialogError(e, "Error completing task")
            }
        }
    }

    private fun processTaskCompletionFromCompletionDialog(taskId: String, endpoint: String) {
        Timber.d("processTaskCompletionFromCompletionDialog called with taskId: $taskId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Timber.d("Completing task from completion dialog...")
                updateState {
                    it.copy(
                        isProcessingAction = true,
                        taskCompletionResult = null
                    )
                }
                Timber.d("State updated - isProcessingAction: true")

                val result = taskXUseCases.completeTask(taskId, endpoint)
                updateState { it.copy(isProcessingAction = false) }
                
                if (result.isSuccess) {
                    // Задача успешно завершена без userMessage
                    TaskXDataHolderSingleton.forceClean()
                    sendEvent(TaskXDetailEvent.NavigateBack)
                } else {
                    val error = result.exceptionOrNull()
                    handleTaskCompletionError(error)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing task: ${e.message}")
                updateState { it.copy(isProcessingAction = false) }
                handleTaskCompletionError(e)
            }
        }
    }

    private fun handleTaskCompletionError(error: Throwable?) {
        when (error) {
            is TaskCompletionException -> {
                val userMessage = error.userMessage
                if (!userMessage.isNullOrBlank()) {
                    if (error.isSuccess) {
                        // Успешное завершение с userMessage - очищаем данные
                        TaskXDataHolderSingleton.forceClean()
                    }
                    updateState {
                        it.copy(
                            taskCompletionResult = TaskCompletionResult(
                                message = userMessage,
                                isSuccess = error.isSuccess
                            )
                        )
                    }
                } else {
                    sendEvent(TaskXDetailEvent.ShowSnackbar(error.message ?: "Error completing task"))
                }
            }
            else -> {
                val errorMessage = error?.message ?: "Error completing task"
                updateState {
                    it.copy(
                        taskCompletionResult = TaskCompletionResult(
                            message = errorMessage,
                            isSuccess = false
                        )
                    )
                }
            }
        }
    }

    fun hideCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = false) }
    }

    fun showCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = true) }
    }

    fun onTaskCompletionOkClick() {
        val isSuccess = uiState.value.taskCompletionResult?.isSuccess ?: false
        updateState { 
            it.copy(
                taskCompletionResult = null,
                showCompletionDialog = false
            )
        }
        
        if (isSuccess) {
            // Успешное завершение - переходим к списку заданий
            sendEvent(TaskXDetailEvent.NavigateBack)
        } else {
            // Ошибка - остаемся на текущем экране
            // Диалог просто закрывается
        }
    }

    fun checkTaskCompletion() {
        val currentState = uiState.value
        val task = currentState.task ?: return

        val allActionsCompleted = task.plannedActions.all { it.isFullyCompleted(task.factActions) }

        Timber.d("checkTaskCompletion: allCompleted=$allActionsCompleted, showDialog=${currentState.showCompletionDialog}, isProcessing=${currentState.isProcessingAction}, hasResult=${currentState.taskCompletionResult != null}")

        // Не показываем диалог завершения, если:
        // - уже показан диалог завершения
        // - обрабатывается завершение задания
        // - показывается результат завершения
        // - показан диалог выхода
        if (allActionsCompleted && 
            !currentState.showCompletionDialog && 
            !currentState.isProcessingAction && 
            currentState.taskCompletionResult == null &&
            !currentState.showExitDialog) {
            Timber.d("Showing completion dialog")
            updateState { it.copy(showCompletionDialog = true) }
        }
    }

    private fun updateFilterState() {
        val activeFilters = TaskXDataHolderSingleton.actionsFilter.getActiveFilters(
            uiState.value.taskType?.searchActionFieldsTypes
        )

        updateState {
            it.copy(
                activeFilters = activeFilters,
                showSearchBar = it.task?.taskType?.isActionSearchEnabled() == true || activeFilters.isNotEmpty()
            )
        }
    }

    fun toggleSearchBar() {
        updateState {
            it.copy(
                showSearchBar = !it.showSearchBar,
                // Если скрываем панель поиска, очищаем значение и ошибку
                searchValue = if (it.showSearchBar) "" else it.searchValue,
                searchError = if (it.showSearchBar) null else it.searchError
            )
        }
    }

    /**
     * Устанавливает значение для поиска
     */
    fun setSearchValue(value: String) {
        updateState { it.copy(searchValue = value) }
    }

    /**
     * Очищает значение поиска
     */
    fun clearSearchValue() {
        updateState { it.copy(searchValue = "", searchError = null) }
    }

    /**
     * Выполняет поиск по введенному тексту
     */
    fun searchByText(value: String) {
        if (value.isBlank()) {
            updateState { it.copy(searchError = "Enter a search value") }
            return
        }

        search(value)
    }

    /**
     * Выполняет поиск по отсканированному штрихкоду
     */
    fun searchByScanner(barcode: String) {
        search(barcode)
    }

    /**
     * Базовый метод поиска
     */
    private fun search(value: String) {
        val task = uiState.value.task ?: return

        updateState { it.copy(isSearching = true, searchError = null) }

        launchIO {
            try {
                val result = actionSearchService.searchActions(
                    value = value,
                    task = task,
                    currentFilter = TaskXDataHolderSingleton.actionsFilter
                )

                handleSearchResult(result)
            } catch (e: Exception) {
                Timber.e(e, "Error searching: $value")
                updateState {
                    it.copy(
                        isSearching = false,
                        searchError = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleSearchResult(result: SearchResult) {
        when (result) {
            is SearchResult.Success -> {
                Timber.d("Found ${result.actionIds.size} actions for field ${result.field}")

                // Сохраняем последний добавленный фильтр
                lastAddedFilterField = result.field

                // Проверяем, нужно ли сохранить в буфер
                val searchFieldType = uiState.value.task?.taskType?.searchActionFieldsTypes
                    ?.find { it.actionField == result.field }

                val saveToBuffer = searchFieldType?.saveToTaskBuffer == true

                // Добавляем фильтр и сохраняем в буфер, если нужно
                TaskXDataHolderSingleton.addFilterAndSaveToBuffer(
                    field = result.field,
                    value = result.value,
                    saveToBuffer = saveToBuffer
                )

                if (saveToBuffer) {
                    filterObjectsAddedToBuffer.add(result.field)
                }

                // Обновляем состояние фильтров в UI
                updateFilterState()
                // Обновляем состояние буфера
                updateBufferState()

                // Очищаем поле поиска
                updateState {
                    it.copy(
                        isSearching = false,
                        searchValue = "",
                        searchError = null
                    )
                }

                // Если найдено только одно действие, открываем визард
                if (result.actionIds.size == 1) {
                    val actionId = result.actionIds.first()
                    sendEvent(TaskXDetailEvent.NavigateToActionWizard(taskId, actionId))
                }
            }

            is SearchResult.NotFound -> {
                updateState {
                    it.copy(
                        isSearching = false,
                        searchError = result.message
                    )
                }
            }

            is SearchResult.Error -> {
                updateState {
                    it.copy(
                        isSearching = false,
                        searchError = result.message
                    )
                }
            }
        }
    }

    /**
     * Обновляет состояние буфера задания в UI
     */
    private fun updateBufferState() {
        val bufferItems = TaskXDataHolderSingleton.taskBuffer.getActiveBufferItems()

        updateState {
            it.copy(
                bufferItems = bufferItems,
                showBufferItems = bufferItems.isNotEmpty()
            )
        }
    }

    /**
     * Удаляет элемент из буфера задания
     */
    fun removeBufferItem(field: FactActionField) {
        TaskXDataHolderSingleton.taskBuffer.clearField(field)
        updateBufferState()
    }

    /**
     * Очищает весь буфер задания
     */
    fun clearAllBufferItems() {
        TaskXDataHolderSingleton.taskBuffer.clear()
        updateBufferState()
    }

    /**
     * Переключает отображение элементов буфера
     */
    fun toggleBufferDisplay() {
        updateState {
            it.copy(showBufferItems = !it.showBufferItems)
        }
    }

    fun toggleFilters() {
        updateState {
            it.copy(showFilters = !it.showFilters)
        }
    }

    /**
     * Удаляет фильтр по указанному полю
     */
    fun removeFilter(field: FactActionField) {
        // Удаляем фильтр и объект из буфера, если он был добавлен из фильтра
        TaskXDataHolderSingleton.removeFilterAndClearFromBuffer(field)

        // Удаляем поле из отслеживаемых
        filterObjectsAddedToBuffer.remove(field)

        // Если это был последний добавленный фильтр, очищаем его
        if (lastAddedFilterField == field) {
            lastAddedFilterField = null
        }

        // Обновляем состояние фильтров и буфера
        updateFilterState()
        updateBufferState()
    }

    /**
     * Очищает все фильтры
     */
    fun clearAllFilters() {
        // Очищаем все фильтры и объекты из буфера
        TaskXDataHolderSingleton.clearAllFiltersAndBufferObjects()

        // Очищаем отслеживаемые поля
        filterObjectsAddedToBuffer.clear()
        lastAddedFilterField = null

        // Обновляем состояние фильтров и буфера
        updateFilterState()
        updateBufferState()
    }

    /**
     * Очищает последний добавленный фильтр
     * Вызывается после возврата из визарда
     */
    fun clearLastAddedFilter() {
        lastAddedFilterField?.let { field ->
            // Удаляем фильтр и объект из буфера, если он был добавлен из фильтра
            TaskXDataHolderSingleton.removeFilterAndClearFromBuffer(field)

            // Удаляем поле из отслеживаемых
            filterObjectsAddedToBuffer.remove(field)
            lastAddedFilterField = null

            // Обновляем состояние фильтров
            updateFilterState()
        }
    }

    /**
     * Обработка возврата из визарда
     * Вызывается при возврате на экран деталей задания
     */
    fun onReturnFromWizard() {
        // Очищаем последний добавленный фильтр, если он привел к открытию визарда
        TaskXDataHolderSingleton.clearLastAddedFilter()
        updateFilterState() // Обновляем состояние фильтров в UI
        updateBufferState() // Обновляем состояние буфера
    }

    fun dismissCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
    }

    fun toggleActionStatus(actionUI: PlannedActionUI, completed: Boolean) {
        val task = uiState.value.task ?: return

        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Task must be in 'In Progress' status"))
            return
        }

        updateState { it.copy(isProcessingAction = true) }

        launchIO {
            try {
                val result = taskXUseCases.setPlannedActionStatus(task.id, actionUI.id, completed, endpoint)

                when (result) {
                    is ApiResult.Success -> {
                        // Обновляем действие в локальном состоянии
                        val updatedPlannedActions = task.plannedActions.map { action ->
                            if (action.id == actionUI.id) {
                                action.copy(
                                    manuallyCompleted = completed,
                                    manuallyCompletedAt = if (completed) LocalDateTime.now() else null
                                )
                            } else {
                                action
                            }
                        }

                        val updatedTask = task.copy(
                            plannedActions = updatedPlannedActions,
                            lastModifiedAt = LocalDateTime.now()
                        )

                        // Обновляем задание в TaskXDataHolderSingleton
                        TaskXDataHolderSingleton.updateTask(updatedTask)

                        // Создаем новые UI-модели для действий
                        val actionUiModels = createActionUiModels(updatedTask)

                        // Обновляем состояние ViewModel
                        updateState {
                            it.copy(
                                task = updatedTask,
                                actionUiModels = actionUiModels,
                                isProcessingAction = false
                            )
                        }

                        // Показываем уведомление
                        val message = if (completed) "Action marked as completed" else "Completion mark removed"
                        sendEvent(TaskXDetailEvent.ShowSnackbar(message))
                    }
                    is ApiResult.Error -> {
                        updateState { it.copy(isProcessingAction = false) }
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Error: ${result.message}"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error changing action status: ${e.message}")
                updateState { it.copy(isProcessingAction = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    fun onFastForwardClick() {
        // Временно меняем фильтр на "Текущие", чтобы получить текущие действия
        val currentFilter = uiState.value.actionFilter
        updateState { it.copy(actionFilter = ActionFilter.CURRENT) }

        // Получаем первое текущее действие
        val currentActions = uiState.value.getDisplayActions()

        // Восстанавливаем оригинальный фильтр
        updateState { it.copy(actionFilter = currentFilter) }

        if (currentActions.isNotEmpty()) {
            // Открываем визард для первого текущего действия
            val firstAction = currentActions.first()
            Timber.d("Opening current action via FAB: ${firstAction.name}")
            onActionClick(firstAction.id)
        } else {
            // Если нет текущих действий, показываем сообщение
            sendEvent(TaskXDetailEvent.ShowSnackbar("No available current actions"))
        }
    }
}