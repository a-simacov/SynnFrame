package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber

class DynamicTaskDetailViewModel(
    private val taskId: String, // Теперь принимаем ID задания вместо объекта
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val userUseCases: UserUseCases,
    private val taskContextManager: TaskContextManager
) : BaseViewModel<DynamicTaskDetailState, DynamicTaskDetailEvent>(
    DynamicTaskDetailState() // Начинаем с пустого состояния
) {

    init {
        loadTaskDetails() // Загружаем детали при инициализации
    }

    private fun loadTaskDetails() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val detailsEndpoint = "$endpoint/$taskId/details"

                val result = dynamicMenuUseCases.getTaskDetails(detailsEndpoint, taskId)

                if (result.isSuccess()) {
                    val taskDetails = result.getOrNull()
                    if (taskDetails != null) {
                        updateState {
                            it.copy(
                                task = taskDetails,
                                isLoading = false,
                                error = null
                            )
                        }

                        checkTaskStatus(taskDetails)
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Не удалось получить детали задания"
                            )
                        }
                    }
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка загрузки деталей задания: $error"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке деталей задания")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    private fun checkTaskStatus(task: DynamicTask) {
        launchIO {
            try {
                val currentUser = userUseCases.getCurrentUser().first()
                val taskStatus = task.getTaskStatus()
                val isCurrentUserExecutor = task.executorId == currentUser?.id

                if ((taskStatus == TaskXStatus.IN_PROGRESS || taskStatus == TaskXStatus.PAUSED) &&
                    isCurrentUserExecutor) {
                    startTaskExecution()
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при проверке статуса задания")
            }
        }
    }

    fun onStartTaskExecution() {
        startTaskExecution()
    }

    private fun startTaskExecution() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val startEndpoint = "$endpoint/$taskId/start"

                val result = dynamicMenuUseCases.startDynamicTask(startEndpoint, taskId)

                if (result.isSuccess()) {
                    val startResponse = result.getOrNull()
                    if (startResponse != null) {
                        taskContextManager.saveStartedTask(startResponse, endpoint)
                        navigateToTaskXDetail(startResponse.task.id)
                    } else {
                        sendEvent(DynamicTaskDetailEvent.ShowSnackbar("Не удалось получить данные для запуска задания"))
                    }
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка запуска задания: $error")
                    sendEvent(DynamicTaskDetailEvent.ShowSnackbar("Ошибка запуска задания: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при запуске задания")
                sendEvent(DynamicTaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun navigateToTaskXDetail(taskId: String) {
        sendEvent(DynamicTaskDetailEvent.NavigateToTaskXDetail(taskId))
    }
}