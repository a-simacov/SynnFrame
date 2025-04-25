package com.synngate.synnframe.presentation.ui.dynamicmenu

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicTasksViewModel(
    val menuItemId: String,
    val menuItemName: String,
    val endpoint: String,
    val screenSettings: ScreenSettings,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
) : BaseViewModel<DynamicTasksState, DynamicTasksEvent>(
    DynamicTasksState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        endpoint = endpoint,
        screenSettings = screenSettings
    )
) {

    init {
        if (uiState.value.hasElement(ScreenElementType.SHOW_LIST)) {
            loadDynamicTasks()
        }
    }

    fun loadDynamicTasks() {
        val currentEndpoint = uiState.value.endpoint
        if (currentEndpoint.isEmpty()) {
            Timber.e("Cannot load tasks: endpoint is empty")
            updateState { it.copy(error = "Ошибка загрузки заданий: эндпоинт не указан") }
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getDynamicTasks(currentEndpoint)
                if (result.isSuccess()) {
                    val tasks = result.getOrNull() ?: emptyList()

                    if (tasks.size == 1 && uiState.value.screenSettings.openImmediately) {
                        sendEvent(DynamicTasksEvent.NavigateToTaskDetail(tasks[0]))
                    }

                    updateState {
                        it.copy(
                            tasks = tasks,
                            isLoading = false,
                            error = if (tasks.isEmpty()) "Нет доступных заданий" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки заданий: ${(result as? ApiResult.Error)?.message}"
                    Timber.e(errorMessage)
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading dynamic tasks")
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
        val currentEndpoint = uiState.value.endpoint

        if (currentEndpoint.isEmpty()) {
            sendEvent(DynamicTasksEvent.ShowSnackbar("Ошибка: эндпоинт не указан"))
            return
        }

        if (searchValue.isEmpty()) {
            sendEvent(DynamicTasksEvent.ShowSnackbar("Пожалуйста, введите значение для поиска"))
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                //val result = dynamicMenuUseCases.searchDynamicTask(currentEndpoint, searchValue)
                val params = mapOf(Pair("value", searchValue))
                val result = dynamicMenuUseCases.getDynamicTasks(currentEndpoint, params)

                if (result.isSuccess()) {
                    val tasks = result.getOrNull()
                    if (tasks != null) {
                        val task = tasks[0]
                        updateState { it.copy(foundTask = task, isLoading = false) }
                        sendEvent(DynamicTasksEvent.NavigateToTaskDetail(task))
                    } else {
                        updateState { it.copy(
                            error = "Задание не найдено",
                            isLoading = false
                        ) }
                    }
                } else {
                    val error = result as? ApiResult.Error
                    updateState { it.copy(
                        error = error?.message ?: "Ошибка поиска",
                        isLoading = false
                    ) }
                    sendEvent(DynamicTasksEvent.ShowSnackbar(error?.message ?: "Ошибка поиска"))
                }
            } catch (e: Exception) {
                updateState { it.copy(
                    error = "Ошибка: ${e.message}",
                    isLoading = false
                ) }
                sendEvent(DynamicTasksEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    fun onRefresh() {
        loadDynamicTasks()
    }
}