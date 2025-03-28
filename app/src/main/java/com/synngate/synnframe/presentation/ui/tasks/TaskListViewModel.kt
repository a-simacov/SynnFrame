package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TaskListViewModel(
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskListState, TaskListEvent>(TaskListState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        observeTasksCount()
        loadTasks()
    }

    private fun observeTasksCount() {
        launchIO {
            taskUseCases.getTasksCountForCurrentUser().collectLatest { count ->
                updateState { it.copy(tasksCount = count) }
            }
        }
    }

    private fun loadTasks() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val state = uiState.value
                val currentUser = userUseCases.getCurrentUser().first()

                val nameFilter = state.searchQuery.takeIf { it.isNotEmpty() }

                taskUseCases.getFilteredTasks(
                    nameFilter = nameFilter,
                    statusFilter = state.selectedStatusFilters.takeIf { it.isNotEmpty() }?.toList(),
                    typeFilter = state.selectedTypeFilters.takeIf { it.isNotEmpty() }?.toList(),
                    dateFromFilter = state.dateFromFilter,
                    dateToFilter = state.dateToFilter,
                    executorIdFilter = currentUser?.id
                ).catch { e ->
                    Timber.e(e, "Error loading tasks")
                    updateState { it.copy(
                        isLoading = false,
                        error = "Unknown error occurred: ${e.message}"
                    ) }
                }.collect { tasks ->
                    updateState { it.copy(
                        tasks = tasks,
                        isLoading = false,
                        error = null
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading tasks")
                updateState { it.copy(
                    isLoading = false,
                    error = "Unknown error occurred: ${e.message}"
                ) }
                loggingService.logError("Error loading logs: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        loadTasks()
    }

    fun formatTaskType(type: TaskType): String {
        return when (type) {
            TaskType.RECEIPT -> "Приемка"
            TaskType.PICK -> "Отбор"
        }
    }

    fun formatStatusType(status: TaskStatus): String {
        return when (status) {
            TaskStatus.TO_DO -> "К выполнению"
            TaskStatus.IN_PROGRESS -> "Выполняется"
            TaskStatus.COMPLETED -> "Выполнено"
        }
    }

    fun updateTypeFilter(types: Set<TaskType>) {
        updateState { it.copy(selectedTypeFilters = types) }
        loadTasks()
    }

    fun updateStatusFilter(statuses: Set<TaskStatus>) {
        updateState { it.copy(selectedStatusFilters = statuses) }
        loadTasks()
    }

    fun toggleFilterPanel() {
        updateState { it.copy(isFilterPanelVisible = !it.isFilterPanelVisible) }
    }

    fun onTaskClick(taskId: String) {
        sendEvent(TaskListEvent.NavigateToTaskDetail(taskId))
    }

    fun syncTasks() {
        launchIO {
            updateState { it.copy(isSyncing = true) }

            try {
                val result = taskUseCases.syncTasks()

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    val formattedTime = LocalDateTime.now().format(dateFormatter)

                    updateState { it.copy(
                        isSyncing = false,
                        lastSyncTime = formattedTime
                    ) }

                    sendEvent(TaskListEvent.ShowSnackbar("Синхронизация выполнена. Обновлено $count заданий."))
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"

                    updateState { it.copy(isSyncing = false) }
                    sendEvent(TaskListEvent.ShowSnackbar("Ошибка синхронизации: $error"))
                    loggingService.logError("Ошибка синхронизации: $error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing tasks")

                updateState { it.copy(isSyncing = false) }
                sendEvent(TaskListEvent.ShowSnackbar("Ошибка синхронизации: ${e.message}"))
                loggingService.logError("Ошибка синхронизации: ${e.message}")
            }
        }
    }

    fun createNewTask() {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Получаем текущего пользователя
                val currentUser = userUseCases.getCurrentUser().first()

                if (currentUser == null) {
                    sendEvent(TaskListEvent.ShowSnackbar("Для создания задания необходимо авторизоваться"))
                    updateState { it.copy(isProcessing = false) }
                    return@launchIO
                }

                // Создаем новое задание с базовыми параметрами
                val taskId = UUID.randomUUID().toString()
                val newTask = Task(
                    id = taskId,
                    name = "Новое задание",
                    type = TaskType.RECEIPT, // По умолчанию приёмка
                    barcode = "NEW-$taskId",
                    createdAt = LocalDateTime.now(),
                    creationPlace = CreationPlace.APP,
                    executorId = currentUser.id,
                    status = TaskStatus.TO_DO
                )

                // Добавляем задание
                val result = taskUseCases.addTask(newTask)

                if (result.isSuccess) {
                    sendEvent(TaskListEvent.ShowSnackbar("Задание успешно создано"))
                    sendEvent(TaskListEvent.NavigateToTaskDetail(newTask.id))
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(TaskListEvent.ShowSnackbar("Ошибка создания задания: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating task")
                sendEvent(TaskListEvent.ShowSnackbar("Ошибка создания задания: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }
}