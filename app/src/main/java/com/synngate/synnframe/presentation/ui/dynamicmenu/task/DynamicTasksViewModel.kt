package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.common.search.SearchResultType
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
        loadDynamicTasks()
    }

    fun loadDynamicTasks() {
        launchIO {
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
                                error = if (tasks.isEmpty()) "Нет доступных заданий" else null
                            )
                        }
                        Timber.d("Loaded ${tasks.size} tasks, taskTypeId: $taskTypeId")
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Получен пустой ответ от сервера"
                            )
                        }
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка загрузки заданий: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке заданий")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при загрузке заданий: ${e.message}"
                    )
                }
            }
        }
    }

    fun createNewTask() {
        val taskTypeId = uiState.value.taskTypeId ?: return

        launchIO {
            updateState { it.copy(isCreatingTask = true, error = null) }

            try {
                Timber.d("Creating new task with taskTypeId: $taskTypeId")
                val result = dynamicMenuUseCases.createTask(endpoint, taskTypeId)

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
                                error = "Ошибка: пустой ответ при создании задания"
                            )
                        }
                    }
                } else {
                    val errorMsg = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    Timber.e("Error creating task: $errorMsg")
                    updateState {
                        it.copy(
                            isCreatingTask = false,
                            error = "Ошибка создания задания: $errorMsg"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while creating task")
                updateState {
                    it.copy(
                        isCreatingTask = false,
                        error = "Ошибка при создании задания: ${e.message}"
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
                Timber.e(e, "Ошибка при запуске задания")
                sendEvent(DynamicTasksEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearch() {
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
                    Timber.d("Локальный поиск успешно нашел ${localSearchResults.size} заданий")

                    updateState {
                        it.copy(
                            tasks = localSearchResults,
                            // Если найдено ровно одно задание, устанавливаем его как foundTask
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
                Timber.d("Локальный поиск не дал результатов, выполняем удаленный поиск")

                val searchEndpoint = endpoint
                val result = dynamicMenuUseCases.searchDynamicTask(searchEndpoint, searchValue)

                if (result.isSuccess()) {
                    val task = result.getOrNull()
                    if (task != null) {
                        updateState {
                            it.copy(
                                tasks = listOf(task),
                                foundTask = task,
                                isLoading = false,
                                error = null,
                                lastSearchQuery = searchValue,
                                searchResultType = SearchResultType.REMOTE,
                                isLocalSearch = false
                            )
                        }

                        // Если настройка openImmediately = true и нашли ровно одно задание
                        if (uiState.value.screenSettings.openImmediately) {
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
                                error = "Задание не найдено",
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
                            error = "Ошибка поиска: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске задания")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при поиске задания: ${e.message}"
                    )
                }
            }
        }
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
        updateState { it.copy(searchValue = value) }
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
}