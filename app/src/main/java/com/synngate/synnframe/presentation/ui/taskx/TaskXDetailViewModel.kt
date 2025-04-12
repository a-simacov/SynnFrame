package com.synngate.synnframe.presentation.ui.taskx

import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.service.WizardController
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailEvent
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailState
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import com.synngate.synnframe.presentation.ui.wizard.builder.WizardBuilder
import com.synngate.synnframe.presentation.ui.wizard.component.BinSelectionFactory
import com.synngate.synnframe.presentation.ui.wizard.component.ExpirationDateFactory
import com.synngate.synnframe.presentation.ui.wizard.component.LabelPrintingFactory
import com.synngate.synnframe.presentation.ui.wizard.component.PalletClosingFactory
import com.synngate.synnframe.presentation.ui.wizard.component.PalletCreationFactory
import com.synngate.synnframe.presentation.ui.wizard.component.PalletSelectionFactory
import com.synngate.synnframe.presentation.ui.wizard.component.ProductSelectionFactory
import com.synngate.synnframe.presentation.ui.wizard.component.ProductStatusFactory
import com.synngate.synnframe.presentation.ui.wizard.component.QuantityInputFactory
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskXDetailViewModel(
    private val taskId: String,
    private val taskXUseCases: TaskXUseCases,
    private val userUseCases: UserUseCases,
    val factLineWizardViewModel: FactLineWizardViewModel,
    val wizardController: WizardController
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val wizardBuilder = WizardBuilder()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")


    init {
        loadTask()
        setupWizardBuilder()
    }

    private fun setupWizardBuilder() {
        wizardBuilder.registerStepComponent(
            FactLineXActionType.SELECT_PRODUCT,
            ProductSelectionFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.ENTER_QUANTITY,
            QuantityInputFactory()
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.SELECT_BIN,
            BinSelectionFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.SELECT_PALLET,
            PalletSelectionFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.CREATE_PALLET,
            PalletCreationFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.CLOSE_PALLET,
            PalletClosingFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.PRINT_LABEL,
            LabelPrintingFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.SELECT_PRODUCT_STATUS,
            ProductStatusFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.ENTER_EXPIRATION_DATE,
            ExpirationDateFactory()
        )
    }

    fun startAddFactLineWizard() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            if (task.status != TaskXStatus.IN_PROGRESS) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Добавление строк факта возможно только для задания в статусе 'Выполняется'"))
                return@launchIO
            }

            updateState { it.copy(isProcessing = true) }

            try {
                wizardController.cancel()
                factLineWizardViewModel.clearCache()
                wizardController.initialize(task, wizardBuilder)

                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskXDetailEvent.ShowFactLineWizard)
            } catch (e: Exception) {
                Timber.e(e, "Error initilizing wizard: ${e.message}")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error initilizing wizard: ${e.message}"
                    )
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

    fun processWizardStep(result: Any?) {
        wizardController.processStepResult(result)
    }

    fun completeWizard() {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = wizardController.completeWizard()

                if (result.isSuccess) {
                    loadTask()
                    updateState {
                        it.copy(isProcessing = false)
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Строка факта успешно добавлена"))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Error on creating fact row"
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = errorMessage
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error on completing wizard")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Error on creating fact row: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Error on creating fact row"))
            }
        }
    }

    fun cancelWizard() {
        launchIO {
            sendEvent(TaskXDetailEvent.HideFactLineWizard)
            cleanupWizardResources()
        }
    }

    private fun cleanupWizardResources() {
        launchIO {
            try {
                wizardController.cancel()
                factLineWizardViewModel.clearCache()
            } catch (e: Exception) {
                Timber.e(e, "Error on clearing wizard resources")
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
        return uiState.value.taskType?.name ?: "Unknown type"
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

    override fun dispose() {
        super.dispose()
        wizardController.dispose()
    }
}