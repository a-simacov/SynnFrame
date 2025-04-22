package com.synngate.synnframe.presentation.ui.operation

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.OperationMenuType
import com.synngate.synnframe.domain.usecase.operation.OperationMenuUseCases
import com.synngate.synnframe.presentation.ui.operation.model.OperationTasksEvent
import com.synngate.synnframe.presentation.ui.operation.model.OperationTasksState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class OperationTasksViewModel(
    val operationId: String,
    val operationName: String,
    val operationType: OperationMenuType,
    private val operationMenuUseCases: OperationMenuUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<OperationTasksState, OperationTasksEvent>(
    OperationTasksState(
        operationId = operationId,
        operationName = operationName,
        operationType = operationType
    )
) {

    init {
        if (operationType == OperationMenuType.SHOW_LIST) {
            loadOperationTasks()
        }
    }

    fun loadOperationTasks() {
        val currentOperationId = uiState.value.operationId
        if (currentOperationId.isEmpty()) {
            Timber.e("Cannot load tasks: operationId is empty")
            updateState { it.copy(error = "Ошибка загрузки заданий: ID операции не указан") }
            return
        }

        launchIO {
            Timber.d("Loading tasks for operation: $currentOperationId")
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = operationMenuUseCases.getOperationTasks(currentOperationId)
                if (result.isSuccess()) {
                    val tasks = result.getOrNull() ?: emptyList()
                    Timber.d("Tasks loaded: ${tasks.size} items")
                    updateState {
                        it.copy(
                            tasks = tasks,
                            isLoading = false,
                            error = if (tasks.isEmpty()) "Нет доступных заданий" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки заданий: ${result as? com.synngate.synnframe.data.remote.api.ApiResult.Error}"
                    Timber.e(errorMessage)
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading operation tasks")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки заданий: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchValueChanged(value: String) {
        updateState { it.copy(searchValue = value) }
    }

    fun onSearch() {
        val searchValue = uiState.value.searchValue.trim()

        if (searchValue.isEmpty()) {
            sendEvent(OperationTasksEvent.ShowSnackbar("Please enter a search value"))
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = operationMenuUseCases.searchTaskByValue(operationId, searchValue)

                if (result.isSuccess()) {
                    val task = result.getOrNull()
                    if (task != null) {
                        updateState { it.copy(foundTask = task, isLoading = false) }
                        sendEvent(OperationTasksEvent.NavigateToTaskDetail(task))
                    } else {
                        updateState { it.copy(
                            error = "No task found",
                            isLoading = false
                        ) }
                    }
                } else {
                    val error = result as? ApiResult.Error
                    updateState { it.copy(
                        error = error?.message ?: "Search failed",
                        isLoading = false
                    ) }
                    sendEvent(OperationTasksEvent.ShowSnackbar(error?.message ?: "Search failed"))
                }
            } catch (e: Exception) {
                updateState { it.copy(
                    error = "Error: ${e.message}",
                    isLoading = false
                ) }
                sendEvent(OperationTasksEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    fun onBackClick() {
        Timber.d("Back clicked in operation tasks")
        sendEvent(OperationTasksEvent.NavigateBack)
    }

    fun onRefresh() {
        Timber.d("Refreshing operation tasks")
        loadOperationTasks()
    }
}