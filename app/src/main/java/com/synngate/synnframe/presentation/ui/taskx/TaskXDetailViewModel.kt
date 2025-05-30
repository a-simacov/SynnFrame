package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.filter.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.service.ActionSearchService
import com.synngate.synnframe.presentation.ui.taskx.validator.ActionValidator
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class TaskXDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val productUseCases: ProductUseCases,
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val actionValidator = ActionValidator()
    private val actionSearchService = ActionSearchService(productUseCases)
    // Отслеживание объектов, добавленных в буфер из фильтра
    private val filterObjectsAddedToBuffer = mutableSetOf<FactActionField>()
    // Отслеживание последнего добавленного фильтра
    private var lastAddedFilterField: FactActionField? = null

    init {
        loadTask()
        loadCurrentUser()
        updateFilterState()
        updateBufferState()
    }

    private fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val startEndpoint = "$endpoint/$taskId/take"
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
                        Timber.e("Задание или его тип не определены после загрузки")
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Не удалось загрузить данные задания"
                            )
                        }
                        sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки данных задания"))
                    }
                } else {
                    val error = (taskResult as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка при загрузке задания: $error")
                    updateState { it.copy(isLoading = false, error = error) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка загрузки: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Исключение при загрузке задания $taskId")
                updateState { it.copy(isLoading = false, error = e.message) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
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

    fun onActionClick(actionId: String) {
        val task = uiState.value.task ?: return
        val actionUiModel = uiState.value.actionUiModels.find { it.id == actionId } ?: return

        if (!actionUiModel.isClickable) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Действие уже выполнено"))
            return
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            sendEvent(TaskXDetailEvent.ShowSnackbar("Задание должно быть в статусе 'Выполняется'"))
            return
        }

        val validationResult = actionValidator.canExecuteAction(task, actionId)
        if (!validationResult.isSuccess) {
            showValidationError(validationResult.errorMessage ?: "Невозможно выполнить действие")
            return
        }

        if (!TaskXDataHolderSingleton.hasData()) {
            TaskXDataHolderSingleton.setTaskData(task, task.taskType!!, endpoint)

            if (!TaskXDataHolderSingleton.hasData()) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: невозможно запустить визард. Данные недоступны."))
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
        updateState { it.copy(showExitDialog = false) }
    }

    fun exitWithoutSaving() {
        dismissExitDialog()
        TaskXDataHolderSingleton.forceClean()
        sendEvent(TaskXDetailEvent.NavigateBack)
    }

    fun continueWork() {
        dismissExitDialog()
    }

    private fun processTaskAction(
        action: suspend (String, String) -> Result<TaskX>,
        loadingMessage: String,
        successMessage: String,
        errorMessage: String,
        onSuccess: (TaskX) -> Unit = {
            sendEvent(TaskXDetailEvent.NavigateBackWithMessage(successMessage))
        }
    ) {
        val task = uiState.value.task ?: return

        updateState { it.copy(isProcessingAction = true, showExitDialog = false, showCompletionDialog = false) }

        launchIO {
            try {
                Timber.d(loadingMessage)
                val result = action(task.id, endpoint)

                if (result.isSuccess) {
                    result.getOrNull()?.let { updatedTask ->
                        updateState { it.copy(task = updatedTask) }
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    onSuccess(task)
                } else {
                    updateState { it.copy(isProcessingAction = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "$loadingMessage: ${e.message}")
                updateState { it.copy(isProcessingAction = false) }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun pauseTask() {
        processTaskAction(
            action = { taskId, endpoint -> taskXUseCases.pauseTask(taskId, endpoint) },
            loadingMessage = "Приостановка задания...",
            successMessage = "Задание приостановлено",
            errorMessage = "Ошибка при приостановке задания"
        )
    }

    fun completeTask() {
        val task = uiState.value.task ?: return
        val taskType = uiState.value.taskType ?: return

        val validationResult = actionValidator.canCompleteTask(task)
        if (!validationResult.isSuccess && !taskType.allowCompletionWithoutFactActions) {
            sendEvent(TaskXDetailEvent.ShowSnackbar(
                validationResult.errorMessage ?: "Невозможно завершить задание"
            ))
            return
        }

        processTaskAction(
            action = { taskId, endpoint -> taskXUseCases.completeTask(taskId, endpoint) },
            loadingMessage = "Завершение задания...",
            successMessage = "Задание завершено",
            errorMessage = "Ошибка при завершении задания",
            onSuccess = {
                TaskXDataHolderSingleton.forceClean()
                sendEvent(TaskXDetailEvent.NavigateBackWithMessage("Задание завершено"))
            }
        )
    }

    fun hideCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = false) }
    }

    fun showCameraScannerForSearch() {
        updateState { it.copy(showCameraScannerForSearch = true) }
    }

    fun checkTaskCompletion() {
        val task = uiState.value.task ?: return

        val allActionsCompleted = task.plannedActions.all { it.isFullyCompleted(task.factActions) }

        if (allActionsCompleted && !uiState.value.showCompletionDialog) {
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
            updateState { it.copy(searchError = "Введите значение для поиска") }
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
                Timber.e(e, "Ошибка при поиске: $value")
                updateState {
                    it.copy(
                        isSearching = false,
                        searchError = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleSearchResult(result: SearchResult) {
        when (result) {
            is SearchResult.Success -> {
                Timber.d("Найдено ${result.actionIds.size} действий по полю ${result.field}")

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
}