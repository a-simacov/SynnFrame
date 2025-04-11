package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXListEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskXListViewModel(
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases
) : BaseViewModel<TaskXListState, TaskXListEvent>(TaskXListState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        loadTasks()
    }

    fun loadTasks() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }
            val currentUser = userUseCases.getCurrentUser().first()
            val state = uiState.value

            try {
                taskXUseCases.getFilteredTasks(
                    nameFilter = state.searchQuery,
                    statusFilter = state.selectedStatuses.takeIf { it.isNotEmpty() }?.toList(),
                    typeFilter = state.selectedTypes.takeIf { it.isNotEmpty() }?.toList(),
                    dateFromFilter = state.dateFromFilter,
                    dateToFilter = state.dateToFilter,
                    executorIdFilter = currentUser?.id
                ).catch { e ->
                    Timber.e(e, "Error loading task list")
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error loading task list: ${e.message}"
                        )
                    }
                }.collect { filteredTasks ->
                    updateState {
                        Timber.d("Uploaded tasks X: ${filteredTasks.size}")
                        it.copy(
                            tasks = filteredTasks,
                            isLoading = false,
                            error = null,
                            hasActiveFilters = state.searchQuery.isNotEmpty() ||
                                    state.selectedStatuses.isNotEmpty() ||
                                    state.selectedTypes.isNotEmpty() ||
                                    state.dateFromFilter != null ||
                                    state.dateToFilter != null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading tasks")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error uploading tasks: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        loadTasks()
    }

    fun updateStatusFilter(statuses: Set<TaskXStatus>) {
        updateState { it.copy(selectedStatuses = statuses) }
        loadTasks()
    }

    fun updateTypeFilter(types: Set<String>) {
        updateState { it.copy(selectedTypes = types) }
        loadTasks()
    }

    fun updateDateFilter(dateFrom: LocalDateTime?, dateTo: LocalDateTime?) {
        updateState { it.copy(dateFromFilter = dateFrom, dateToFilter = dateTo) }
        loadTasks()
    }

    fun clearFilters() {
        updateState { it.copy(
            searchQuery = "",
            selectedStatuses = emptySet(),
            selectedTypes = emptySet(),
            dateFromFilter = null,
            dateToFilter = null
        ) }
        loadTasks()
    }

    fun navigateToTaskDetail(taskId: String) {
        sendEvent(TaskXListEvent.NavigateToTaskDetail(taskId))
    }

    fun showDateFilterDialog() {
        updateState { it.copy(isDateFilterDialogVisible = true) }
    }

    fun hideDateFilterDialog() {
        updateState { it.copy(isDateFilterDialogVisible = false) }
    }

    fun formatDate(dateTime: LocalDateTime): String {
        return dateTime.format(dateFormatter)
    }

    fun formatTaskStatus(status: TaskXStatus): String {
        return when (status) {
            TaskXStatus.TO_DO -> "К выполнению"
            TaskXStatus.IN_PROGRESS -> "Выполняется"
            TaskXStatus.PAUSED -> "Приостановлено"
            TaskXStatus.COMPLETED -> "Завершено"
            TaskXStatus.CANCELLED -> "Отменено"
        }
    }

    fun formatTaskType(task: TaskX): String {
        return task.taskTypeId
    }
}