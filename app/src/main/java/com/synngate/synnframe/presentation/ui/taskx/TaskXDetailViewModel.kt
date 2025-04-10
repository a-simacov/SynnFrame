package com.synngate.synnframe.presentation.ui.taskx

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.service.FactLineWizardController
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskXDetailViewModel(
    private val taskId: String,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    private val factLineWizardViewModel: FactLineWizardViewModel,
    val factLineWizardController: FactLineWizardController
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        loadTask()

        // Подписываемся на изменения состояния мастера
        viewModelScope.launch {
            factLineWizardController.wizardState.collectLatest { wizardState ->
                // Обновляем UI при изменении состояния мастера
                if (wizardState != null) {
                    sendEvent(TaskXDetailEvent.ShowFactLineWizard)
                }
            }
        }
    }

    fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val task = taskXUseCases.getTaskById(taskId)

                if (task != null) {
                    // Загружаем тип задания
                    val taskType = taskXUseCases.getTaskType(task.taskTypeId)

                    // Получаем текущего пользователя
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
                            error = "Задание с ID $taskId не найдено"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки задания")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки задания: ${e.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Ошибка при начале выполнения задания"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при начале выполнения задания")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Ошибка при начале выполнения задания: ${e.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Ошибка при завершении задания"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при завершении задания")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showCompletionDialog = false,
                        error = "Ошибка при завершении задания: ${e.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Ошибка при приостановке задания"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при приостановке задания")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Ошибка при приостановке задания: ${e.message}"
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
                            error = result.exceptionOrNull()?.message ?: "Ошибка при возобновлении задания"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при возобновлении задания")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Ошибка при возобновлении задания: ${e.message}"
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
                    // Перезагружаем задание, чтобы обновить поле isVerified
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
                Timber.e(e, "Ошибка при верификации задания")
                updateState {
                    it.copy(
                        isProcessing = false,
                        showVerificationDialog = false,
                        error = "Ошибка при верификации задания: ${e.message}"
                    )
                }
            }
        }
    }

    fun startAddFactLineWizard() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            if (task.status != TaskXStatus.IN_PROGRESS) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Добавление строк факта возможно только для задания в статусе 'Выполняется'"))
                return@launchIO
            }

            factLineWizardViewModel.clearCache()

            // Инициализируем мастер через контроллер
            factLineWizardController.initialize(task)
        }
    }

    fun processWizardStep(result: Any?) {
        launchIO {
            factLineWizardController.processStepResult(result)
        }
    }

    fun completeWizard() {
        launchIO {
            val result = factLineWizardController.completeWizard()

            if (result.isSuccess) {
                // Перезагружаем задание для отображения новой строки факта
                loadTask()
                sendEvent(TaskXDetailEvent.ShowSnackbar("Строка факта успешно добавлена"))
            } else {
                updateState {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Ошибка при добавлении строки факта"
                    )
                }
            }
        }
    }

    fun cancelWizard() {
        factLineWizardController.cancelWizard()
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

    fun showPlanLines() {
        updateState { it.copy(activeView = TaskXDetailView.PLAN_LINES) }
    }

    fun showFactLines() {
        updateState { it.copy(activeView = TaskXDetailView.FACT_LINES) }
    }

    fun showComparedLines() {
        updateState { it.copy(activeView = TaskXDetailView.COMPARED_LINES) }
    }

    fun isActionAvailable(action: AvailableTaskAction): Boolean {
        val taskType = uiState.value.taskType ?: return false
        return action in taskType.availableActions
    }
}