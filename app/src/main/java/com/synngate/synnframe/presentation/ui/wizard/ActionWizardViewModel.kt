package com.synngate.synnframe.presentation.ui.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ViewModel для экрана визарда действий.
 * Управляет состоянием визарда и интерфейсом пользователя.
 */
class ActionWizardViewModel(
    val taskId: String,
    val actionId: String,
    val wizardStateMachine: WizardStateMachine,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry
) : BaseViewModel<Unit, ActionWizardEvent>(Unit), Disposable {
    private val TAG = "ActionWizardViewModel"
    private var observingJob: Job? = null

    // Флаг для предотвращения ложных срабатываний в начале работы визарда
    private val safeToNavigateBack = AtomicBoolean(false)

    init {
        Timber.d("$TAG: Инициализация, taskId=$taskId, actionId=$actionId")
        viewModelScope.launch {
            try {
                // Запускаем инициализацию
                val initResult = wizardStateMachine.initialize(taskId, actionId)

                if (!initResult.isSuccess) {
                    // Если инициализация не удалась, обрабатываем ошибку и навигируем назад
                    val error = initResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Timber.e("$TAG: Ошибка инициализации: $error")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $error"))
                    sendEvent(ActionWizardEvent.NavigateBack)
                } else {
                    Timber.d("$TAG: Инициализация успешно завершена")

                    // Добавляем значительную задержку (2 секунды) перед активацией
                    // механизма автоматической навигации назад
                    viewModelScope.launch {
                        delay(2000) // 2 секунды
                        Timber.d("$TAG: Активация безопасного режима навигации назад")
                        safeToNavigateBack.set(true)
                        startObservingState()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка инициализации визарда: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка инициализации: ${e.message}"))
                sendEvent(ActionWizardEvent.NavigateBack)
            }
        }
    }

    private fun startObservingState() {
        // Отменяем предыдущую задачу, если она существует
        observingJob?.cancel()

        Timber.d("$TAG: Запуск наблюдения за состоянием")
        observingJob = viewModelScope.launch {
            try {
                wizardStateMachine.state
                    .map { it.isUninitialized && wizardStateMachine.isInitialized() && safeToNavigateBack.get() }
                    .distinctUntilChanged()
                    .collectLatest { isReset ->
                        if (isReset) {
                            Timber.d("$TAG: Обнаружен сброс состояния, выполняем навигацию назад")
                            sendEvent(ActionWizardEvent.NavigateBack)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка при наблюдении за состоянием: ${e.message}")
            }
        }
    }

    fun completeWizard() {
        launchIO {
            try {
                val result = wizardStateMachine.complete()

                if (result.isSuccess) {
                    sendEvent(ActionWizardEvent.NavigateBackWithSuccess(actionId))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка завершения визарда: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка завершения: ${e.message}"))
            }
        }
    }

    fun retryCompleteWizard() {
        completeWizard()
    }

    fun cancelWizard() {
        wizardStateMachine.cancel()
        sendEvent(ActionWizardEvent.NavigateBack)
    }

    fun goBackToPreviousStep() {
        launchIO {
            try {
                wizardStateMachine.processStepResult(null)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка навигации: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка навигации: ${e.message}"))
            }
        }
    }

    fun processBarcodeFromScanner(barcode: String) {
        wizardStateMachine.processBarcodeFromScanner(barcode)
    }

    fun processStepResult(result: Any?) {
        launchIO {
            try {
                wizardStateMachine.processStepResult(result)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка обработки результата шага: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    /**
     * Освобождает ресурсы, используемые ViewModel.
     * Вызывает dispose() на stateMachine и actionStepFactoryRegistry.
     */
    override fun dispose() {
        Timber.d("$TAG: Освобождение ресурсов")
        super.dispose()

        try {
            // Отменяем задачу наблюдения
            observingJob?.cancel()
            observingJob = null

            // Сначала очищаем кэши всех фабрик
            actionStepFactoryRegistry.clearAllCaches()

            // Затем освобождаем ресурсы stateMachine
            wizardStateMachine.dispose()

            // Наконец, освобождаем ресурсы реестра фабрик
            actionStepFactoryRegistry.dispose()

            Timber.d("$TAG: Ресурсы успешно освобождены")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка при освобождении ресурсов")
        }
    }

    /**
     * Метод для явного освобождения ресурсов, который может быть вызван из UI
     */
    fun onDispose() {
        dispose()
    }
}