package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
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
    private val taskTypeUseCases: TaskTypeUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskListState, TaskListEvent>(TaskListState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        observeTasksCount()
        loadTaskTypes()
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

    private fun loadTaskTypes() {
        launchIO {
            taskTypeUseCases.getTaskTypes().collectLatest { types ->
                updateState { it.copy(availableTaskTypes = types) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        loadTasks()
    }

    fun formatStatusType(status: TaskStatus): String {
        return when (status) {
            TaskStatus.TO_DO -> "К выполнению"
            TaskStatus.IN_PROGRESS -> "Выполняется"
            TaskStatus.COMPLETED -> "Выполнено"
        }
    }

    // Обновляем метод для фильтрации
    fun updateTypeFilter(typeIds: Set<String>) {
        updateState { it.copy(selectedTypeFilters = typeIds) }
        loadTasks()
    }

    // Функция получения имени типа по идентификатору
    fun getTaskTypeName(typeId: String): String {
        return uiState.value.availableTaskTypes
            .find { it.id == typeId }?.name ?: typeId
    }

    // Заменяем старую реализацию
    fun formatTaskType(typeId: String): String {
        // Получаем название типа из списка доступных типов
        val taskType = uiState.value.availableTaskTypes.find { it.id == typeId }
        return taskType?.name ?: typeId
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

                // Получаем идентификатор типа "Приемка" из доступных типов заданий
                val receiptTypeId = uiState.value.availableTaskTypes
                    .find { it.name == "Приемка" || it.id == "RECEIPT" }?.id
                    ?: "RECEIPT" // Запасной вариант, если тип не найден

                val taskId = UUID.randomUUID().toString()
                val newTask = Task(
                    id = taskId,
                    name = "Новое задание",
                    taskTypeId = receiptTypeId, // Используем найденный ID типа
                    barcode = "NEW-$taskId",
                    createdAt = LocalDateTime.now(),
                    creationPlace = CreationPlace.APP,
                    executorId = currentUser.id,
                    status = TaskStatus.TO_DO,
                    allowProductsNotInPlan = true  // Устанавливаем признак для новых заданий
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