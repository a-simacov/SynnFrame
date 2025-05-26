package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
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

    init {
        loadDynamicTasks()
    }

    fun loadDynamicTasks() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getDynamicTasks(endpoint)

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
                val result = dynamicMenuUseCases.startDynamicTask(startEndpoint, taskId)

                if (result.isSuccess()) {
                    val taskX = result.getOrNull()
                    if (taskX != null && taskX.taskType != null) {
                        sendEvent(DynamicTasksEvent.SetTaskDataAndNavigate(
                            task = taskX,
                            taskType = taskX.taskType,
                            endpoint = endpoint
                        ))
                    } else {
                        sendEvent(DynamicTasksEvent.ShowSnackbar("Не удалось получить данные для запуска задания"))
                    }
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка запуска задания: $error")
                    sendEvent(DynamicTasksEvent.ShowSnackbar("Ошибка запуска задания: $error"))
                }
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
                                error = null
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
                                error = "Задание не найдено"
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

    fun onSearchValueChanged(value: String) {
        updateState { it.copy(searchValue = value) }
    }

    private fun navigateToTaskDetail(taskId: String) {
        sendEvent(DynamicTasksEvent.NavigateToTaskDetail(taskId, endpoint))
    }

    private fun navigateToTaskXDetail(taskId: String) {
        sendEvent(DynamicTasksEvent.NavigateToTaskXDetail(taskId))
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}