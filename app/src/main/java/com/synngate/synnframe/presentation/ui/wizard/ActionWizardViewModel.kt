// Заменяет com.synngate.synnframe.presentation.ui.wizard.ActionWizardViewModel
package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

/**
 * Упрощенная ViewModel для экрана визарда действий.
 * Использует WizardStateMachine напрямую без прослоек-адаптеров.
 */
class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    val wizardStateMachine: WizardStateMachine,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<Unit, ActionWizardEvent>(Unit), Disposable {

    init {
        initializeWizard()
        // Наблюдаем за состоянием визарда для отправки событий UI
        launchIO {
            wizardStateMachine.state.collectLatest { state ->
                if (state == null) {
                    // Если состояние стало null, это означает отмену визарда
                    sendEvent(ActionWizardEvent.NavigateBack)
                }
                // Здесь можно добавить дополнительную логику обработки изменений состояния
            }
        }
    }

    /**
     * Инициализирует визард для текущего действия
     */
    private fun initializeWizard() {
        launchIO {
            try {
                val result = wizardStateMachine.initialize(taskId, actionId)

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
     * Завершает визард и выполняет действие
     */
    fun completeWizard() {
        launchIO {
            try {
                val result = wizardStateMachine.complete()

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

    /**
     * Повторное выполнение действия после ошибки
     */
    fun retryCompleteWizard() {
        completeWizard()
    }

    /**
     * Отменяет визард
     */
    fun cancelWizard() {
        wizardStateMachine.cancel()
        sendEvent(ActionWizardEvent.NavigateBack)
    }

    /**
     * Возврат к предыдущему шагу
     */
    fun goBackToPreviousStep() {
        launchIO {
            try {
                wizardStateMachine.processStepResult(null)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при возврате к предыдущему шагу: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка навигации: ${e.message}"))
            }
        }
    }

    /**
     * Обработка штрих-кода от сканера
     */
    fun processBarcodeFromScanner(barcode: String) {
        launchIO {
            wizardStateMachine.processBarcodeFromScanner(barcode)
        }
    }

    /**
     * Обработка результата шага
     */
    fun processStepResult(result: Any?) {
        launchIO {
            try {
                wizardStateMachine.processStepResult(result)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке результата шага: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    override fun dispose() {
        super.dispose()
        wizardStateMachine.dispose()
    }
}