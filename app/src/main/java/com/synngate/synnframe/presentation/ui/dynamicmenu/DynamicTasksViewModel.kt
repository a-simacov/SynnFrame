package com.synngate.synnframe.presentation.ui.dynamicmenu

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicTasksViewModel(
    val menuItemId: String,
    val menuItemName: String,
    val menuItemType: DynamicMenuItemType,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
) : BaseViewModel<DynamicTasksState, DynamicTasksEvent>(
    DynamicTasksState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        menuItemType = menuItemType
    )
) {

    init {
        if (menuItemType == DynamicMenuItemType.SHOW_LIST) {
            loadDynamicTasks()
        }
    }

    fun loadDynamicTasks() {
        val currentMenuItemId = uiState.value.menuItemId
        if (currentMenuItemId.isEmpty()) {
            Timber.e("Cannot load tasks: operationId is empty")
            updateState { it.copy(error = "Ошибка загрузки заданий: ID операции не указан") }
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getOperationTasks(currentMenuItemId)
                if (result.isSuccess()) {
                    val tasks = result.getOrNull() ?: emptyList()
                    updateState {
                        it.copy(
                            tasks = tasks,
                            isLoading = false,
                            error = if (tasks.isEmpty()) "Нет доступных заданий" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки заданий: ${result as? ApiResult.Error}"
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
            sendEvent(DynamicTasksEvent.ShowSnackbar("Please enter a search value"))
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.searchTaskByValue(menuItemId, searchValue)

                if (result.isSuccess()) {
                    val task = result.getOrNull()
                    if (task != null) {
                        updateState { it.copy(foundTask = task, isLoading = false) }
                        sendEvent(DynamicTasksEvent.NavigateToTaskDetail(task))
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
                    sendEvent(DynamicTasksEvent.ShowSnackbar(error?.message ?: "Search failed"))
                }
            } catch (e: Exception) {
                updateState { it.copy(
                    error = "Error: ${e.message}",
                    isLoading = false
                ) }
                sendEvent(DynamicTasksEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    fun onRefresh() {
        loadDynamicTasks()
    }
}