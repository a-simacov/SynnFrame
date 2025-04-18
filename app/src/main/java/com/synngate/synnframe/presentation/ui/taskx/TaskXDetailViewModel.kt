package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskXDetailViewModel(
    private val taskId: String,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    val actionWizardController: ActionWizardController,
    val actionWizardContextFactory: ActionWizardContextFactory,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        loadTask()
    }

    // Метод для запуска визарда выполнения действия
    fun startActionExecution(actionId: String) {
        launchIO {
            try {
                val result = actionWizardController.initialize(taskId, actionId)
                if (result.isSuccess) {
                    // Обновляем состояние, чтобы показать визард
                    updateState { it.copy(showActionWizard = true) }
                } else {
                    updateState { it.copy(error = "Ошибка при инициализации действия: ${result.exceptionOrNull()?.message}") }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось начать выполнение действия"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing action wizard")
                updateState { it.copy(error = "Ошибка при инициализации действия: ${e.message}") }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось начать выполнение действия"))
            }
        }
    }

    // Метод для скрытия визарда
    fun hideActionWizard() {
        updateState { it.copy(showActionWizard = false) }
        // Отменяем визард при скрытии
        actionWizardController.cancel()
    }

    // Метод для завершения визарда
    fun completeActionWizard() {
        launchIO {
            try {
                val result = actionWizardController.complete()
                if (result.isSuccess) {
                    loadTask() // Перезагрузка задания
                    updateState { it.copy(showActionWizard = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Действие успешно выполнено"))
                } else {
                    updateState { it.copy(error = "Ошибка при выполнении действия: ${result.exceptionOrNull()?.message}") }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось выполнить действие"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing action wizard")
                updateState { it.copy(error = "Ошибка при завершении действия: ${e.message}") }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Не удалось выполнить действие"))
            }
        }
    }

    fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val task = taskXUseCases.getTaskById(taskId)

                if (task != null) {
                    val taskType = taskXUseCases.getTaskType(task.taskTypeId)
                    val currentUser = userUseCases.getCurrentUser().first()

                    updateState {
                        it.copy(
                            task = task,
                            taskType = taskType,
                            currentUserId = currentUser?.id,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Task by ID $taskId was not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error loading task: ${e.message}"
                    )
                }
            }
        }
    }

    fun startTask() {
        launchIO {
            val currentState = uiState.value
            val task = currentState.task ?: return@launchIO
            val currentUserId = currentState.currentUserId ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.startTask(task.id, currentUserId)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание начато"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on starting task"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on starting task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on starting task: ${e.message}"
                    )
                }
            }
        }
    }

    fun completeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.completeTask(task.id)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false,
                            showCompletionDialog = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание завершено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            showCompletionDialog = false,
                            error = result.exceptionOrNull()?.message ?: "Error on completing task"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on completing task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showCompletionDialog = false,
                        error = "Error on completing task: ${e.message}"
                    )
                }
            }
        }
    }

    fun pauseTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.pauseTask(task.id)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание приостановлено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on pausing task"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on pausing task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on pausing task: ${e.message}"
                    )
                }
            }
        }
    }

    fun resumeTask() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.resumeTask(task.id)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    updateState {
                        it.copy(
                            task = updatedTask,
                            isProcessing = false
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание возобновлено"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = result.exceptionOrNull()?.message ?: "Error on resuming task"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on resuming task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on resuming task: ${e.message}"
                    )
                }
            }
        }
    }

    fun verifyTask(barcode: String) {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskXUseCases.verifyTask(task.id, barcode)

                if (result.isSuccess && result.getOrNull() == true) {
                    loadTask()
                    updateState { it.copy(showVerificationDialog = false) }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Задание успешно верифицировано"))
                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            showVerificationDialog = false,
                            error = "Неверный штрихкод для верификации"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on verification task")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showVerificationDialog = false,
                        error = "Error on verification task: ${e.message}"
                    )
                }
            }
        }
    }

    fun showCompletionDialog() {
        updateState { it.copy(showCompletionDialog = true) }
    }

    fun hideCompletionDialog() {
        updateState { it.copy(showCompletionDialog = false) }
    }

    fun showVerificationDialog() {
        updateState { it.copy(showVerificationDialog = true) }
    }

    fun hideVerificationDialog() {
        updateState { it.copy(showVerificationDialog = false) }
    }

    fun formatDate(dateTime: LocalDateTime?): String {
        return dateTime?.format(dateFormatter) ?: "Не указано"
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

    fun formatTaskType(taskTypeId: String): String {
        return uiState.value.taskType?.name ?: "Неизвестный тип"
    }

    // Методы для переключения видов
    fun showPlannedActions() {
        updateState { it.copy(activeView = TaskXDetailView.PLANNED_ACTIONS) }
    }

    fun showFactActions() {
        updateState { it.copy(activeView = TaskXDetailView.FACT_ACTIONS) }
    }

    fun isActionAvailable(action: AvailableTaskAction): Boolean {
        val taskType = uiState.value.taskType ?: return false
        return action in taskType.availableActions
    }

    override fun dispose() {
        super.dispose()
    }
}