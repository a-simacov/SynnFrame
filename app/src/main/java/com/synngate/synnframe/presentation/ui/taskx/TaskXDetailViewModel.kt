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
    val wizardController: WizardController  // Переименовываем с factLineWizardController на wizardController
) : BaseViewModel<TaskXDetailState, TaskXDetailEvent>(TaskXDetailState()) {

    private val wizardBuilder = WizardBuilder()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")


    init {
        loadTask()

        // Регистрация фабрик компонентов
        setupWizardBuilder()
    }

    private fun setupWizardBuilder() {
        // Регистрация всех типов компонентов
        wizardBuilder.registerStepComponent(
            FactLineXActionType.SELECT_PRODUCT,
            ProductSelectionFactory(factLineWizardViewModel)
        )
        wizardBuilder.registerStepComponent(
            FactLineXActionType.ENTER_QUANTITY,
            QuantityInputFactory(factLineWizardViewModel)
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
            ExpirationDateFactory(factLineWizardViewModel)
        )
    }

    // Метод для запуска визарда
    fun startAddFactLineWizard() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            if (task.status != TaskXStatus.IN_PROGRESS) {
                sendEvent(TaskXDetailEvent.ShowSnackbar("Добавление строк факта возможно только для задания в статусе 'Выполняется'"))
                return@launchIO
            }

            updateState { it.copy(isProcessing = true) }

            try {
                // Полная очистка перед запуском
                factLineWizardViewModel.clearCache()

                // Даем время для очистки кэша
                kotlinx.coroutines.delay(300)

                // Инициализируем мастер
                wizardController.initialize(task, wizardBuilder)

                // Ещё немного ждем для убеждения в стабильности
                kotlinx.coroutines.delay(300)

                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskXDetailEvent.ShowFactLineWizard)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при инициализации визарда")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Ошибка при инициализации визарда: ${e.message}"
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

    fun processWizardStep(result: Any?) {
        wizardController.processStepResult(result)
    }

    /**
     * Завершение работы визарда и создание строки факта
     */
    /**
     * Завершение работы визарда и создание строки факта
     */
    fun completeWizard() {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = wizardController.completeWizard()

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()

                    // Загружаем задание заново для обновления списка строк факта
                    loadTask()

                    updateState {
                        it.copy(isProcessing = false)
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar("Строка факта успешно добавлена"))

                    // Не закрываем визард, чтобы можно было добавить еще строки
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Ошибка при создании строки факта"
                    updateState {
                        it.copy(
                            isProcessing = false,
                            error = errorMessage
                        )
                    }
                    sendEvent(TaskXDetailEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при завершении визарда")
                updateState {
                    it.copy(
                        isProcessing = false,
                        error = "Ошибка при создании строки факта: ${e.message}"
                    )
                }
                sendEvent(TaskXDetailEvent.ShowSnackbar("Ошибка при создании строки факта"))
            }
        }
    }

    fun cancelWizard() {
        launchIO {
            try {
                // Очищаем состояние визарда
                wizardController.cancel()

                // Очищаем кэш данных
                factLineWizardViewModel.clearCache()

                // Даем время для завершения очистки
                kotlinx.coroutines.delay(200)

                // Только после очистки отправляем событие скрытия UI
                launchMain {
                    sendEvent(TaskXDetailEvent.HideFactLineWizard)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при отмене визарда")
                // Все равно пытаемся скрыть UI
                launchMain {
                    sendEvent(TaskXDetailEvent.HideFactLineWizard)
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