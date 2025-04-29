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
    val task: DynamicTask,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val userUseCases: UserUseCases,
    private val endpoint: String // Добавлен параметр endpoint для выполнения запросов
) : BaseViewModel<DynamicTaskDetailState, DynamicTaskDetailEvent>(
    DynamicTaskDetailState(task = task)
) {

    init {
        // При создании проверяем статус задания и текущего пользователя
        // для возможного автоматического перехода к выполнению
        checkTaskStatus()
    }

    private fun checkTaskStatus() {
        // Проверяем, можно ли автоматически перейти к выполнению задания
        launchIO {
            try {
                val currentUser = userUseCases.getCurrentUser().first()
                val taskStatus = task.getTaskStatus()
                val isCurrentUserExecutor = task.executorId == currentUser?.id

                if ((taskStatus == TaskXStatus.IN_PROGRESS || taskStatus == TaskXStatus.PAUSED) &&
                    isCurrentUserExecutor) {
                    // Если задание уже выполняется текущим пользователем,
                    // автоматически переходим к экрану выполнения
                    navigateToTaskXDetail(task.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при проверке статуса задания")
            }
        }
    }

    fun onStartTaskExecution() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                Timber.d("Запуск задания: ${task.id} через endpoint: $endpoint")
                val result = dynamicMenuUseCases.startDynamicTask(endpoint, task.id)

                if (result.isSuccess()) {
                    val startResponse = result.getOrNull()
                    if (startResponse != null) {
                        Timber.d("Задание успешно запущено, переходим к TaskXDetail: ${startResponse.task.id}")
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