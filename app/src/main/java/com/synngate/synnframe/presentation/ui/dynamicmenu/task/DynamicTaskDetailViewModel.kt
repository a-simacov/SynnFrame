package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber

class DynamicTaskDetailViewModel(
    private val taskId: String,
    private val endpoint: String,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val userUseCases: UserUseCases,
) : BaseViewModel<DynamicTaskDetailState, DynamicTaskDetailEvent>(
    DynamicTaskDetailState()
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
                                error = "Failed to get task details"
                            )
                        }
                    }
                } else {
                    val error = (result as? ApiResult.Error)?.message ?: "Unknown error"
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error loading task details: $error"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task details")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
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
                Timber.e(e, "Error checking task status")
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
                val startEndpoint = "$endpoint/$taskId/take"

                // Напрямую передаем в навигацию параметры taskId и endpoint
                // Вместо загрузки и сохранения в холдер
                sendEvent(DynamicTaskDetailEvent.NavigateToTaskXDetail(taskId, endpoint))
            } catch (e: Exception) {
                Timber.e(e, "Error starting task")
                sendEvent(DynamicTaskDetailEvent.ShowSnackbar("Error: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }
}