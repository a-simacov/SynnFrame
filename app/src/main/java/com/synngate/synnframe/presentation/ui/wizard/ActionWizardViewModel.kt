package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

/**
 * Оптимизированная ViewModel для экрана визарда действий.
 * Использует WizardStateMachine напрямую без прослоек-адаптеров.
 */
class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    val wizardStateMachine: WizardStateMachine,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<Unit, ActionWizardEvent>(Unit), Disposable {
    private val TAG = "ActionWizardViewModel"

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
                    WizardLogger.logError(TAG, result.exceptionOrNull() ?: Exception(errorMessage),
                        "инициализации визарда")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                    sendEvent(ActionWizardEvent.NavigateBack)
                }
            } catch (e: Exception) {
                WizardLogger.logError(TAG, e, "инициализации визарда")
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
                Timber.d("Начинаем завершение визарда для actionId=$actionId, taskId=$taskId")
                val result = wizardStateMachine.complete()

                if (result.isSuccess) {
                    WizardLogger.logStep(TAG, "complete", "Визард успешно завершен",
                        WizardLogger.LogLevel.MINIMAL)
                    Timber.d("Отправляем событие NavigateBackWithSuccess с actionId=$actionId")
                    //delay(100)
                    sendEvent(ActionWizardEvent.NavigateBackWithSuccess(actionId))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    WizardLogger.logError(TAG, result.exceptionOrNull() ?: Exception(errorMessage),
                        "завершения визарда")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                }
            } catch (e: Exception) {
                WizardLogger.logError(TAG, e, "завершения визарда")
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
                WizardLogger.logError(TAG, e, "возврата к предыдущему шагу")
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
                WizardLogger.logError(TAG, e, "обработки результата шага")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    override fun dispose() {
        super.dispose()
        wizardStateMachine.dispose()
    }
}