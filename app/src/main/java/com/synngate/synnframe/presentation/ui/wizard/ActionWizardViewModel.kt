package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

/**
 * ViewModel для экрана визарда действий
 */
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

    /**
     * Инициализация визарда
     */
    private fun initializeWizard() {
        launchIO {
            try {
                Timber.d("Инициализация визарда для задания $taskId, действия $actionId")
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

    /**
     * Завершение визарда с отправкой результата
     */
    fun completeWizard() {
        launchIO {
            try {
                Timber.d("Завершение визарда для действия $actionId")
                val result = actionWizardController.complete()

                if (result.isSuccess) {
                    Timber.d("Визард успешно завершен")
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

    /**
     * Повторная попытка завершения визарда
     */
    fun retryCompleteWizard() {
        launchIO {
            try {
                Timber.d("Повторная попытка завершения визарда для действия $actionId")
                val result = actionWizardController.complete()

                if (result.isSuccess) {
                    Timber.d("Визард успешно завершен")
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

    /**
     * Отмена визарда
     */
    fun cancelWizard() {
        Timber.d("Отмена визарда")
        actionWizardController.cancel()
        sendEvent(ActionWizardEvent.NavigateBack)
    }

    /**
     * Обработка штрихкода от сканера
     */
    fun processBarcodeFromScanner(barcode: String) {
        launchIO {
            Timber.d("Обработка штрихкода: $barcode")
            actionWizardController.processBarcodeFromScanner(barcode)
        }
    }

    /**
     * Освобождение ресурсов
     */
    override fun dispose() {
        super.dispose()
        Timber.d("Освобождение ресурсов ActionWizardViewModel")
        actionWizardController.cancel()
    }
}