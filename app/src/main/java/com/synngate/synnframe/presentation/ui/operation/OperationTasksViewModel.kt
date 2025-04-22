package com.synngate.synnframe.presentation.ui.operation

import com.synngate.synnframe.domain.usecase.operation.OperationMenuUseCases
import com.synngate.synnframe.presentation.ui.operation.model.OperationTasksEvent
import com.synngate.synnframe.presentation.ui.operation.model.OperationTasksState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class OperationTasksViewModel(
    operationId: String,
    operationName: String,
    private val operationMenuUseCases: OperationMenuUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<OperationTasksState, OperationTasksEvent>(
    OperationTasksState(
        operationId = operationId,
        operationName = operationName
    )
) {

    init {
        loadOperationTasks()
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

    fun onBackClick() {
        Timber.d("Back clicked in operation tasks")
        sendEvent(OperationTasksEvent.NavigateBack)
    }

    fun onRefresh() {
        Timber.d("Refreshing operation tasks")
        loadOperationTasks()
    }
}