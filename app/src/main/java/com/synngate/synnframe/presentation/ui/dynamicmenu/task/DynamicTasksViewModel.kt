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
    private val menuItemId: String,
    private val menuItemName: String,
    private val endpoint: String,
    private val screenSettings: ScreenSettings,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val userUseCases: UserUseCases // Добавляем UserUseCases для проверки текущего пользователя
) : BaseViewModel<DynamicTasksState, DynamicTasksEvent>(
    DynamicTasksState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        endpoint = endpoint,
        screenSettings = screenSettings
    )
) {

    init {
        loadTasks()
    }

    fun loadTasks() {
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
                    val error = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка загрузки заданий: $error"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке заданий")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun onTaskClick(task: DynamicTask) {
        launchIO {
            try {
                // Получаем текущего пользователя для проверки, является ли он исполнителем
                val currentUser = userUseCases.getCurrentUser().first()

                // Проверяем статус задания и исполнителя
                val taskStatus = task.getTaskStatus()
                val isCurrentUserExecutor = task.executorId == currentUser?.id

                if ((taskStatus == TaskXStatus.IN_PROGRESS || taskStatus == TaskXStatus.PAUSED) &&
                    isCurrentUserExecutor) {
                    // Если задание выполняется или приостановлено и текущий пользователь - исполнитель,
                    // то сразу переходим к экрану выполнения
                    Timber.d("Задание уже выполняется текущим пользователем, переходим к TaskXDetail: ${task.id}")
                    sendEvent(DynamicTasksEvent.NavigateToTaskXDetail(task.id))
                } else {
                    // Иначе открываем экран деталей
                    Timber.d("Открываем экран деталей для задания: ${task.id}")
                    sendEvent(DynamicTasksEvent.NavigateToTaskDetail(task))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке клика по заданию")
                // В случае ошибки просто открываем стандартный экран деталей
                sendEvent(DynamicTasksEvent.NavigateToTaskDetail(task))
            }
        }
    }

    fun onSearchTasks() {
        launchIO {
            val searchValue = uiState.value.searchValue
            if (searchValue.isBlank()) {
                sendEvent(DynamicTasksEvent.ShowSnackbar("Введите значение для поиска"))
                return@launchIO
            }

            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.searchDynamicTask(endpoint, searchValue)

                if (result.isSuccess()) {
                    val task = result.getOrNull()
                    if (task != null) {
                        if (screenSettings.openImmediately) {
                            // Автоматически открываем детальный экран, если настройки разрешают
                            sendEvent(DynamicTasksEvent.NavigateToTaskDetail(task))
                        } else {
                            // Обновляем состояние с найденной задачей
                            updateState {
                                it.copy(
                                    tasks = listOf(task),
                                    isLoading = false,
                                    foundTask = task
                                )
                            }
                        }
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Задание не найдено"
                            )
                        }
                    }
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка поиска: $error"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске задания")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchValueChanged(newValue: String) {
        updateState { it.copy(searchValue = newValue) }
    }

    fun navigateBack() {
        sendEvent(DynamicTasksEvent.NavigateBack)
    }
}