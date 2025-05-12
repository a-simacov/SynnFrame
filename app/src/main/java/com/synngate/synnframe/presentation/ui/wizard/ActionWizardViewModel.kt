package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    val actionWizardController: ActionWizardController,
    val actionWizardContextFactory: ActionWizardContextFactory,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<Unit, ActionWizardEvent>(Unit) {

    init {
        initializeWizard()
    }

    private fun initializeWizard() {
        launchIO {
            try {
                val result = actionWizardController.initialize(taskId, actionId)

                if (!result.isSuccess) {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка инициализации визарда: $errorMessage")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                    sendEvent(ActionWizardEvent.NavigateBack)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при инициализации визарда")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка инициализации: ${e.message}"))
                sendEvent(ActionWizardEvent.NavigateBack)
            }
        }
    }

    fun completeWizard() {
        launchIO {
            try {
                val result = actionWizardController.complete()

                if (result.isSuccess) {
                    sendEvent(ActionWizardEvent.NavigateBackWithSuccess(actionId))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка завершения визарда: $errorMessage")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при завершении визарда")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка завершения: ${e.message}"))
            }
        }
    }

    fun retryCompleteWizard() {
        launchIO {
            try {
                val result = actionWizardController.complete()

                if (result.isSuccess) {
                    sendEvent(ActionWizardEvent.NavigateBackWithSuccess(actionId))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Timber.e("Ошибка повторного завершения визарда: $errorMessage")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при повторном завершении визарда")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка повторной отправки: ${e.message}"))
            }
        }
    }

    fun cancelWizard() {
        actionWizardController.cancel()
        sendEvent(ActionWizardEvent.NavigateBack)
    }

    fun goBackToPreviousStep() {
        launchIO {
            try {
                actionWizardController.processStepResult(null)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при возврате к предыдущему шагу: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка навигации: ${e.message}"))
            }
        }
    }

    fun goBackFromSummary() {
        launchIO {
            try {
                val state = actionWizardController.wizardState.value
                if (state != null && state.isCompleted) {
                    actionWizardController.processStepResult(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при возврате с итогового экрана: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка навигации: ${e.message}"))
            }
        }
    }

    fun processBarcodeFromScanner(barcode: String) {
        launchIO {
            actionWizardController.processBarcodeFromScanner(barcode)
        }
    }

    override fun dispose() {
        super.dispose()
        actionWizardController.cancel()
    }
}