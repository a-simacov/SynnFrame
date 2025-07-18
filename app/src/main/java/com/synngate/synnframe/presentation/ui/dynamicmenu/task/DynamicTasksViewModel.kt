package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.common.search.SearchResultType
import com.synngate.synnframe.presentation.navigation.DynamicMenuDataHolder
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTasksState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber

class DynamicTasksViewModel(
    menuItemId: String,
    menuItemName: String,
    val endpoint: String,
    screenSettings: ScreenSettings,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val userUseCases: UserUseCases
) : BaseViewModel<DynamicTasksState, DynamicTasksEvent>(
    DynamicTasksState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        endpoint = endpoint,
        screenSettings = screenSettings
    )
) {

    private var allLoadedTasks: List<DynamicTask> = emptyList()

    init {
        restoreSavedKey()
        // Не загружаем здесь, так как экран сделает это при первом RESUMED
    }

    fun loadDynamicTasks() {
        launchIO {
            Timber.d("Loading dynamic tasks from endpoint: $endpoint")
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getDynamicTasks(endpoint)

                if (result.isSuccess()) {
                    val responseDto = result.getOrNull()
                    if (responseDto != null) {
                        val tasks = responseDto.list
                        val taskTypeId = responseDto.taskTypeId

                        allLoadedTasks = tasks
                        updateState {
                            it.copy(
                                tasks = tasks,
                                taskTypeId = taskTypeId,
                                isLoading = false,
                            )
                        }
                        Timber.d("Loaded ${tasks.size} tasks, taskTypeId: $taskTypeId")
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Empty response received from server"
                            )
                        }
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error loading tasks: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading tasks")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error loading tasks: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Принудительно обновляет список заданий, игнорируя кеш
     */
    fun forceRefreshTasks() {
        Timber.d("Force refreshing tasks list")
        loadDynamicTasks()
    }

    fun createNewTask() {
        val taskTypeId = uiState.value.taskTypeId ?: return
        val savedSearchKey = if (uiState.value.hasValidSavedSearchKey) uiState.value.savedSearchKey else null

        launchIO {
            updateState { it.copy(isCreatingTask = true, error = null) }

            try {
                Timber.d("Creating new task with taskTypeId: $taskTypeId, savedSearchKey: $savedSearchKey")
                val result = dynamicMenuUseCases.createTask(endpoint, taskTypeId, savedSearchKey)

                if (result.isSuccess()) {
                    val createdTaskX = result.getOrNull()
                    if (createdTaskX != null) {
                        Timber.d("Successfully created new task: ${createdTaskX.id}")

                        // После успешного создания задания переходим на экран выполнения
                        updateState { it.copy(isCreatingTask = false) }
                        sendEvent(DynamicTasksEvent.NavigateToTaskXDetail(createdTaskX.id, endpoint))
                    } else {
                        updateState {
                            it.copy(
                                isCreatingTask = false,
                                error = "Error: empty response when creating task"
                            )
                        }
                    }
                } else {
                    val errorMsg = (result as? ApiResult.Error)?.message ?: "Unknown error"
                    Timber.e("Error creating task: $errorMsg")
                    updateState {
                        it.copy(
                            isCreatingTask = false,
                            error = "Error creating task: $errorMsg"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while creating task")
                updateState {
                    it.copy(
                        isCreatingTask = false,
                        error = "Error creating task: ${e.message}"
                    )
                }
            }
        }
    }

    fun onTaskClick(task: DynamicTask) {
        launchIO {
            val currentUser = userUseCases.getCurrentUser().first()

            // Проверяем статус задания и исполнителя
            val taskStatus = task.getTaskStatus()
            val isCurrentUserExecutor = task.executorId == currentUser?.id

            if ((taskStatus == TaskXStatus.IN_PROGRESS || taskStatus == TaskXStatus.PAUSED) &&
                isCurrentUserExecutor) {
                // Если задание выполняется или приостановлено и текущий пользователь - исполнитель,
                // запускаем задание сразу (без перехода на экран деталей)
                startTask(task.id)
            } else {
                // Иначе переходим к экрану деталей задания
                navigateToTaskDetail(task.id)
            }
        }
    }

    // Метод для запуска задания напрямую
    fun startTask(taskId: String) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val startEndpoint = "$endpoint/$taskId/take"

                // Вместо загрузки данных и сохранения в холдер просто навигируем
                // с передачей необходимых параметров
                sendEvent(DynamicTasksEvent.NavigateToTaskXDetail(taskId, endpoint))
            } catch (e: Exception) {
                Timber.e(e, "Error starting task")
                sendEvent(DynamicTasksEvent.ShowSnackbar("Error: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearch() {
        if (uiState.value.showSavedKeyDialog) {
            Timber.d("Search ignored - saved key dialog is open")
            return
        }

        val searchValue = uiState.value.searchValue
        if (searchValue.isNotEmpty()) {
            searchTask(searchValue)
        } else {
            loadDynamicTasks()
        }
    }

    private fun searchTask(searchValue: String) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // Сначала выполняем локальный поиск
                val localSearchResults = performLocalSearch(searchValue)

                if (localSearchResults.isNotEmpty()) {
                    // Если локальный поиск дал результаты, используем их
                    Timber.d("Local search successfully found ${localSearchResults.size} tasks")

                    updateState {
                        it.copy(
                            tasks = localSearchResults,
                            foundTask = if (localSearchResults.size == 1) localSearchResults.first() else null,
                            isLoading = false,
                            error = null,
                            lastSearchQuery = searchValue,
                            searchResultType = SearchResultType.LOCAL,
                            isLocalSearch = true
                        )
                    }

                    // Если найдено ровно одно задание и включена настройка openImmediately
                    if (localSearchResults.size == 1 && uiState.value.screenSettings.openImmediately) {
                        val task = localSearchResults.first()
                        if (task.getTaskStatus() == TaskXStatus.TO_DO)
                            navigateToTaskDetail(task.id)
                        else
                            navigateToTaskXDetail(task.id)
                    }

                    return@launchIO
                }

                // Если локальный поиск не дал результатов, выполняем удаленный поиск
                Timber.d("Local search returned no results, performing remote search")

                val searchEndpoint = endpoint
                val result = dynamicMenuUseCases.searchDynamicTask(searchEndpoint, searchValue)

                if (result.isSuccess()) {
                    val responseDto = result.getOrNull()
                    if (responseDto != null) {
                        val tasks = responseDto.list
                        val taskTypeId = responseDto.taskTypeId

                        if (tasks.isNotEmpty()) {
                            updateState {
                                it.copy(
                                    tasks = tasks,
                                    foundTask = if (tasks.size == 1) tasks.first() else null,
                                    isLoading = false,
                                    error = null,
                                    lastSearchQuery = searchValue,
                                    searchResultType = SearchResultType.REMOTE,
                                    isLocalSearch = false
                                )
                            }

                            // Если настройка openImmediately = true и нашли ровно одно задание
                            if (uiState.value.screenSettings.openImmediately && tasks.size == 1) {
                                val task = tasks[0]
                                if (task.getTaskStatus() == TaskXStatus.TO_DO)
                                    navigateToTaskDetail(task.id)
                                else
                                    navigateToTaskXDetail(task.id)
                            }
                        } else {
                            updateState {
                                it.copy(
                                    tasks = emptyList(),
                                    isLoading = false,
                                    error = "Task not found",
                                    lastSearchQuery = searchValue,
                                    searchResultType = null,
                                    isLocalSearch = false
                                )
                            }
                        }
                    } else {
                        updateState {
                            it.copy(
                                tasks = emptyList(),
                                isLoading = false,
                                error = "Task not found",
                                lastSearchQuery = searchValue,
                                searchResultType = null,
                                isLocalSearch = false
                            )
                        }
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Search error: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching for task")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error searching for task: ${e.message}"
                    )
                }
            }
        }
    }

    // Новые методы для работы с диалогом сохраняемого ключа
    fun showSavedKeyDialog() {
        // Получаем endpoint для валидации из настроек меню
        val validationEndpoint = extractValidationEndpoint()
        updateState {
            it.copy(
                showSavedKeyDialog = true,
                keyValidationError = null,
                savedKeyEndpoint = validationEndpoint
            )
        }
    }

    fun hideSavedKeyDialog() {
        updateState {
            it.copy(
                showSavedKeyDialog = false,
                keyValidationError = null
            )
        }
    }

    private fun restoreSavedKey() {
        val savedKeyData = DynamicMenuDataHolder.getSavedSearchKey(uiState.value.menuItemId)
        if (savedKeyData != null) {
            updateState {
                it.copy(
                    savedSearchKey = savedKeyData.key,
                    hasValidSavedSearchKey = savedKeyData.isValid
                )
            }
            Timber.d("Restored saved key for menuItemId: ${uiState.value.menuItemId}")
        }
    }

    fun validateAndSaveKey(key: String) {
        val validationEndpoint = uiState.value.savedKeyEndpoint ?: endpoint

        launchIO {
            updateState { it.copy(isValidatingKey = true, keyValidationError = null) }

            try {
                Timber.d("Validating key: $key on endpoint: $validationEndpoint")
                val result = dynamicMenuUseCases.validateSearchKey(validationEndpoint, key)

                if (result.isSuccess()) {
                    val validation = result.getOrNull()
                    if (validation?.isValid == true) {
                        // Ключ валиден, сохраняем его
                        updateState {
                            it.copy(
                                savedSearchKey = validation.validatedValue,
                                hasValidSavedSearchKey = true,
                                showSavedKeyDialog = false,
                                isValidatingKey = false,
                                keyValidationError = null
                            )
                        }

                        // Сохраняем в синглтон
                        DynamicMenuDataHolder.setSavedSearchKey(
                            menuItemId = uiState.value.menuItemId,
                            key = validation.validatedValue ?: key,
                            isValid = true
                        )

                        Timber.d("Key validated and saved successfully")
                    } else {
                        // Ключ не валиден
                        updateState {
                            it.copy(
                                isValidatingKey = false,
                                keyValidationError = validation?.message ?: "Invalid key"
                            )
                        }
                    }
                } else {
                    val errorMsg = (result as? ApiResult.Error)?.message ?: "Validation failed"
                    updateState {
                        it.copy(
                            isValidatingKey = false,
                            keyValidationError = errorMsg
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error validating key")
                updateState {
                    it.copy(
                        isValidatingKey = false,
                        keyValidationError = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSavedSearchKey() {
        updateState {
            it.copy(
                savedSearchKey = null,
                hasValidSavedSearchKey = false
            )
        }

        // Очищаем из синглтона
        DynamicMenuDataHolder.clearSavedSearchKey(uiState.value.menuItemId)

        Timber.d("Saved search key cleared")
    }

    // Вспомогательный метод для извлечения endpoint валидации из настроек
    private fun extractValidationEndpoint(): String {
        // Здесь можно добавить логику извлечения специального endpoint из настроек
        // Пока используем базовый endpoint с суффиксом
        return "$endpoint/validate-key"
    }

    private fun performLocalSearch(searchValue: String): List<DynamicTask> {
        if (searchValue.isBlank() || allLoadedTasks.isEmpty()) {
            return emptyList()
        }

        val normalizedQuery = searchValue.trim().lowercase()

        // Фильтруем задания по поисковому запросу
        return allLoadedTasks.filter { task ->
            task.matchesSearchQuery(normalizedQuery)
        }
    }

    fun onSearchValueChanged(value: String) {
        if (uiState.value.showSavedKeyDialog) {
            Timber.d("Search value change ignored - saved key dialog is open")
            return
        }
        updateState { it.copy(searchValue = value) }
    }

    fun onBarcodeScanned(barcode: String) {
        // Игнорируем сканирование при открытом диалоге
        if (uiState.value.showSavedKeyDialog) {
            Timber.d("Barcode scan ignored - saved key dialog is open")
            return
        }

        onSearchValueChanged(barcode)
        onSearch()
    }

    private fun navigateToTaskDetail(taskId: String) {
        sendEvent(DynamicTasksEvent.NavigateToTaskDetail(taskId, endpoint))
    }

    private fun navigateToTaskXDetail(taskId: String) {
        sendEvent(DynamicTasksEvent.NavigateToTaskXDetail(taskId, endpoint))
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    fun onNavigateBack() {
        // Очищаем сохраненный ключ при выходе с экрана
        DynamicMenuDataHolder.clearSavedSearchKey(uiState.value.menuItemId)
        sendEvent(DynamicTasksEvent.NavigateBack)
    }

    fun onFabClick() {
        if (uiState.value.isSearchSaveable() && !uiState.value.hasValidSavedSearchKey) {
            // Если поиск сохраняемый, но ключа нет - показываем диалог
            showSavedKeyDialog()
        } else {
            // Иначе создаем новое задание
            createNewTask()
        }
    }

    /**
     * Проверяет, нужно ли показывать подсказку о необходимости ввода ключа
     */
    fun shouldShowKeyHint(): Boolean {
        return uiState.value.isSearchSaveable() && !uiState.value.hasValidSavedSearchKey
    }

    fun onTaskLongClick(task: DynamicTask) {
        if (task.isDeletable()) {
            updateState {
                it.copy(
                    showDeleteDialog = true,
                    taskToDelete = task
                )
            }
        }
    }

    fun hideDeleteDialog() {
        updateState {
            it.copy(
                showDeleteDialog = false,
                taskToDelete = null
            )
        }
    }

    fun deleteTask() {
        val task = uiState.value.taskToDelete ?: return

        launchIO {
            updateState { it.copy(isDeleting = true) }

            try {
                val result = dynamicMenuUseCases.deleteTask(endpoint, task.id)

                if (result.isSuccess()) {
                    // Удаляем задание из списка
                    updateState { state ->
                        state.copy(
                            tasks = state.tasks.filter { it.id != task.id },
                            showDeleteDialog = false,
                            taskToDelete = null,
                            isDeleting = false
                        )
                    }

                    // Обновляем allLoadedTasks
                    allLoadedTasks = allLoadedTasks.filter { it.id != task.id }

                    sendEvent(DynamicTasksEvent.ShowSnackbar("Task deleted successfully"))
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Unknown error"
                    updateState {
                        it.copy(
                            isDeleting = false,
                            error = "Error deleting task: $error"
                        )
                    }
                    sendEvent(DynamicTasksEvent.ShowSnackbar("Error deleting task: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting task")
                updateState {
                    it.copy(
                        isDeleting = false,
                        error = "Error deleting task: ${e.message}"
                    )
                }
                sendEvent(DynamicTasksEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }
}