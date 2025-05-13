package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.CancelledState
import com.synngate.synnframe.domain.model.wizard.CompletedState
import com.synngate.synnframe.domain.model.wizard.CompletingState
import com.synngate.synnframe.domain.model.wizard.ErrorState
import com.synngate.synnframe.domain.model.wizard.InitializingState
import com.synngate.synnframe.domain.model.wizard.StepState
import com.synngate.synnframe.domain.model.wizard.WizardEvent
import com.synngate.synnframe.domain.model.wizard.WizardState
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.presentation.di.Disposable
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
    // State flow для UI
    private val _adaptedState = MutableStateFlow<ActionWizardState?>(null)
    val adaptedState: StateFlow<ActionWizardState?> = _adaptedState

    // Создаем собственный CoroutineScope для адаптера
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null

    // Следим за изменениями в FSM и преобразуем их в ActionWizardState
    init {
        // Запускаем наблюдение за состоянием FSM
        startStateObservation()
    }

    /**
     * Начинаем наблюдение за состоянием машины состояний
     */
    private fun startStateObservation() {
        stateObservationJob?.cancel()
        stateObservationJob = adapterScope.launch {
            // Наблюдаем за изменениями в состоянии FSM
            stateMachine.currentState
                .map { state -> adaptStateToActionWizardState(state) }
                .distinctUntilChanged()
                .collect { adaptedState ->
                    _adaptedState.value = adaptedState
                }
        }
    }

    /**
     * Инициализирует визард
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        try {
            val result = stateMachine.initialize(taskId, actionId)
            return if (result) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Failed to initialize wizard"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard")
            return Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага
     */
    suspend fun processStepResult(result: Any?) {
        // Если result == null, то это событие "назад"
        if (result == null) {
            stateMachine.handleEvent(WizardEvent.Back)
        } else {
            stateMachine.handleEvent(WizardEvent.Next(result))
        }
    }

    /**
     * Обрабатывает переход вперед (если результат уже есть)
     */
    fun processForwardStep() {
        // В FSM не предусмотрен явный "процесс вперед",
        // это обрабатывается через Next с существующим результатом
        val state = stateMachine.currentState.value
        val context = stateMachine.wizardContext

        if (state is StepState) {
            val stepId = context.steps.getOrNull(state.stepIndex)?.id
            if (stepId != null && stepId in context.results) {
                adapterScope.launch {
                    stateMachine.handleEvent(WizardEvent.Next(context.results[stepId]!!))
                }
            }
        }
    }

    /**
     * Обрабатывает штрих-код от сканера
     */
    suspend fun processBarcodeFromScanner(barcode: String) {
        stateMachine.handleEvent(WizardEvent.ProcessBarcode(barcode))
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        adapterScope.launch {
            stateMachine.handleEvent(WizardEvent.Cancel)
            stateMachine.reset()
        }
    }

    /**
     * Завершает визард
     */
    suspend fun complete(): Result<TaskX> {
        stateMachine.handleEvent(WizardEvent.Complete)

        // После перехода в состояние "завершено", выполняем действие
        if (stateMachine.currentState.value is CompletedState) {
            return stateMachine.executeAction()
        }

        return Result.failure(IllegalStateException("Failed to complete wizard"))
    }

    /**
     * Адаптирует WizardState в ActionWizardState для совместимости с UI
     */
    private fun adaptStateToActionWizardState(state: WizardState?): ActionWizardState? {
        if (state == null) return null

        val context = stateMachine.wizardContext

        return when (state) {
            is InitializingState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = -1, // -1 означает "инициализация"
                results = context.results,
                isInitialized = false,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = true
            )

            is StepState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = state.stepIndex,
                results = context.results,
                errors = context.errors,
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = false
            )

            is CompletingState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = context.steps.size, // После последнего шага
                results = context.results,
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = true,
                isSending = true
            )

            is CompletedState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = context.steps.size, // После последнего шага
                results = context.results,
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = false,
                isSending = false
            )

            is CancelledState -> null // Состояние отмены возвращает null, что закрывает визард

            is ErrorState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = context.steps.size, // После последнего шага
                results = context.results,
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = false,
                isSending = false,
                sendError = state.errorMessage
            )

            else -> null
        }
    }

    override fun dispose() {
        // Отменяем все корутины при уничтожении адаптера
        stateObservationJob?.cancel()
        adapterScope.cancel()
        stateMachine.reset()
    }
}