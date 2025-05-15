package com.synngate.synnframe.presentation.ui.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Улучшенная ViewModel для экрана визарда действий.
 * Использует WizardStateMachine с ненуллабельным состоянием.
 */
class ActionWizardViewModel(
    private val taskId: String,
    private val actionId: String,
    val wizardStateMachine: WizardStateMachine,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<Unit, ActionWizardEvent>(Unit), Disposable {
    private val TAG = "ActionWizardViewModel"

    init {
        // Этот подход заменяет предыдущий с флагом initializationCompleted и delay
        // Запускаем инициализацию и настраиваем наблюдение за состоянием
        viewModelScope.launch {
            try {
                // Запускаем инициализацию
                val initResult = wizardStateMachine.initialize(taskId, actionId)

                if (!initResult.isSuccess) {
                    // Если инициализация не удалась, обрабатываем ошибку и навигируем назад
                    val error = initResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    WizardLogger.logError(TAG, initResult.exceptionOrNull() ?: Exception(error),
                        "инициализации визарда")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $error"))
                    sendEvent(ActionWizardEvent.NavigateBack)
                }

                // Начинаем наблюдение за состоянием визарда после инициализации
                startObservingState()
            } catch (e: Exception) {
                WizardLogger.logError(TAG, e, "инициализации визарда")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка инициализации: ${e.message}"))
                sendEvent(ActionWizardEvent.NavigateBack)
            }
        }
    }

    /**
     * Настраивает наблюдение за состоянием визарда
     */
    private fun startObservingState() {
        viewModelScope.launch {
            // Наблюдаем за изменениями флага инициализации
            wizardStateMachine.state
                .map { it.isUninitialized && wizardStateMachine.isInitialized() }
                .distinctUntilChanged()
                .collectLatest { isReset ->
                    // Если состояние было сброшено после инициализации, выполняем навигацию назад
                    if (isReset) {
                        Timber.d("Визард был сброшен, выполняем навигацию назад")
                        sendEvent(ActionWizardEvent.NavigateBack)
                    }
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
        wizardStateMachine.processBarcodeFromScanner(barcode)
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