package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Адаптер, интегрирующий WizardStateMachine с ActionWizardController.
 * Преобразует состояния FSM в ActionWizardState для существующего UI.
 */
class FsmWizardControllerAdapter(
    private val stateMachine: WizardStateMachine,
    private val taskContextManager: TaskContextManager,
    private val taskXRepository: TaskXRepository? = null
) : Disposable {
    private val TAG = "FsmWizardControllerAdapter"

    // State flow для UI
    private val _adaptedState = MutableStateFlow<ActionWizardState?>(null)
    val adaptedState: StateFlow<ActionWizardState?> = _adaptedState

    // Создаем собственный CoroutineScope для адаптера
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null

    // Следим за изменениями в FSM и преобразуем их в ActionWizardState
    init {
        // Запускаем наблюдение за состоянием машины состояний
        startStateObservation()
    }

    /**
     * Начинаем наблюдение за состоянием машины состояний
     */
    private fun startStateObservation() {
        stateObservationJob?.cancel()
        stateObservationJob = adapterScope.launch {
            // Наблюдаем за изменениями в состоянии FSM
            stateMachine.state
                .map { state ->
                    Timber.d("$TAG: FSM state changed: ${state?.currentStepIndex ?: "null"}")
                    adaptStateToActionWizardState(state)
                }
                .distinctUntilChanged()
                .collect { adaptedState ->
                    _adaptedState.value = adaptedState
                    if (adaptedState != null) {
                        Timber.d("$TAG: Updated to step ${adaptedState.currentStepIndex}, error=${adaptedState.sendError != null}")
                    }
                }
        }
    }

    /**
     * Инициализирует визард
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        return try {
            val result = stateMachine.initialize(taskId, actionId)

            if (result.isSuccess) {
                Timber.d("$TAG: Successfully initialized wizard for task $taskId, action $actionId")
            } else {
                Timber.e("$TAG: Failed to initialize wizard: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            WizardLogger.logError(TAG, e, "initializing wizard")
            Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага
     */
    suspend fun processStepResult(result: Any?) {
        try {
            Timber.d("$TAG: Processing step result: ${result?.javaClass?.simpleName}")

            if (result == null) {
                // Если result == null, то это событие "назад"
                stateMachine.processStepResult(null)
            } else {
                // Обрабатываем результат напрямую через stateMachine
                stateMachine.processStepResult(result)
            }
        } catch (e: Exception) {
            WizardLogger.logError(TAG, e, "processing step result")
        }
    }

    /**
     * Обрабатывает переход вперед (если результат уже есть)
     */
    fun processForwardStep() {
        adapterScope.launch {
            try {
                val currentState = _adaptedState.value ?: return@launch
                val currentStep = currentState.currentStep ?: return@launch

                // Проверяем, есть ли результат текущего шага
                val stepResult = currentState.results[currentStep.id]
                if (stepResult != null) {
                    Timber.d("$TAG: Auto-forwarding with existing result for step ${currentStep.id}")
                    stateMachine.processStepResult(stepResult)
                }
            } catch (e: Exception) {
                WizardLogger.logError(TAG, e, "processing forward step")
            }
        }
    }

    /**
     * Обрабатывает штрих-код от сканера
     */
    suspend fun processBarcodeFromScanner(barcode: String) {
        stateMachine.processBarcodeFromScanner(barcode)
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        adapterScope.launch {
            stateMachine.cancel()
            stateMachine.reset()
        }
    }

    /**
     * Завершает визард
     */
    suspend fun complete(): Result<TaskX> {
        return try {
            // Выполняем действие через WizardStateMachine
            val result = stateMachine.complete()

            if (result.isSuccess) {
                Timber.d("$TAG: Successfully completed wizard")
            } else {
                Timber.e("$TAG: Failed to complete wizard: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            WizardLogger.logError(TAG, e, "completing wizard")
            Result.failure(e)
        }
    }

    /**
     * Адаптирует WizardState в ActionWizardState для совместимости с UI
     */
    private fun adaptStateToActionWizardState(state: ActionWizardState?): ActionWizardState? {
        return state // Прямое использование состояния из WizardStateMachine
    }

    override fun dispose() {
        // Отменяем все корутины при уничтожении адаптера
        stateObservationJob?.cancel()
        adapterScope.cancel()
        stateMachine.reset()
    }
}