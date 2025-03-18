package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.di.TaskListViewModel
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskListViewModelImpl(
    private val taskUseCases: TaskUseCases,
    private val userUseCases: UserUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskListState, TaskListEvent>(TaskListState()), TaskListViewModel {

    private var filterJob: Job? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        observeTasksCount()
        loadTasks()
    }

    /**
     * Наблюдает за количеством заданий для текущего пользователя
     */
    private fun observeTasksCount() {
        launchIO {
            taskUseCases.getTasksCountForCurrentUser().collectLatest { count ->
                updateState { it.copy(tasksCount = count) }
            }
        }
    }

    /**
     * Загружает и фильтрует задания
     */
    private fun loadTasks() {
        // Отменяем предыдущую загрузку, если она выполняется
        filterJob?.cancel()

        filterJob = launchIO {
            val state = uiState.value

            updateState { it.copy(isLoading = true) }

            try {
                // Получаем текущего пользователя
                val currentUser = userUseCases.getCurrentUser().first()

                // Фильтрация заданий
                taskUseCases.getFilteredTasks(
                    nameFilter = state.searchQuery.takeIf { it.isNotEmpty() },
                    statusFilter = state.selectedStatusFilters.takeIf { it.isNotEmpty() }?.toList(),
                    typeFilter = state.selectedTypeFilter,
                    dateFromFilter = state.dateFromFilter,
                    dateToFilter = state.dateToFilter,
                    executorIdFilter = currentUser?.id
                ).collectLatest { tasks ->
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
                    error = e.message ?: "Unknown error occurred"
                ) }
                sendEvent(TaskListEvent.ShowSnackbar("Ошибка загрузки заданий: ${e.message}"))
            }
        }
    }

    /**
     * Обновляет поисковый запрос и перезагружает задания
     */
    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        loadTasks()
    }

    /**
     * Обрабатывает изменение фильтра по статусу
     */
    fun toggleStatusFilter(status: TaskStatus) {
        val currentFilters = uiState.value.selectedStatusFilters
        val newFilters = if (currentFilters.contains(status)) {
            currentFilters - status
        } else {
            currentFilters + status
        }

        updateState { it.copy(selectedStatusFilters = newFilters) }
        loadTasks()
    }

    /**
     * Очищает все фильтры по статусу
     */
    fun clearStatusFilters() {
        updateState { it.copy(selectedStatusFilters = emptySet()) }
        loadTasks()
    }

    /**
     * Обновляет фильтр по типу задания
     */
    fun updateTypeFilter(type: TaskType?) {
        updateState { it.copy(selectedTypeFilter = type) }
        loadTasks()
    }

    /**
     * Показывает диалог выбора даты для фильтра
     */
    fun showDatePicker(isFromDate: Boolean) {
        val state = uiState.value
        val currentDate = if (isFromDate) state.dateFromFilter else state.dateToFilter

        sendEvent(TaskListEvent.ShowDatePicker(isFromDate, currentDate))
    }

    /**
     * Обновляет дату начала периода для фильтрации
     */
    fun updateDateFromFilter(date: LocalDateTime?) {
        updateState { it.copy(dateFromFilter = date) }
        loadTasks()
    }

    /**
     * Обновляет дату окончания периода для фильтрации
     */
    fun updateDateToFilter(date: LocalDateTime?) {
        updateState { it.copy(dateToFilter = date) }
        loadTasks()
    }

    /**
     * Очищает фильтр по дате
     */
    fun clearDateFilter() {
        updateState { it.copy(
            dateFromFilter = null,
            dateToFilter = null
        ) }
        loadTasks()
    }

    /**
     * Переключает видимость панели фильтров
     */
    fun toggleFilterPanel() {
        updateState { it.copy(isFilterPanelVisible = !it.isFilterPanelVisible) }
    }

    /**
     * Обрабатывает нажатие на задание
     */
    fun onTaskClick(taskId: String) {
        sendEvent(TaskListEvent.NavigateToTaskDetail(taskId))
    }

    /**
     * Синхронизирует задания с сервером
     */
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing tasks")

                updateState { it.copy(isSyncing = false) }
                sendEvent(TaskListEvent.ShowSnackbar("Ошибка синхронизации: ${e.message}"))
            }
        }
    }
}