package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
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
                .map { state ->
                    Timber.d("FSM state changed: ${state?.id}")
                    val adapted = adaptStateToActionWizardState(state)
                    Timber.d("Adapted to ActionWizardState: currentStep=${adapted?.currentStepIndex}, results=${adapted?.results?.size ?: 0}")
                    adapted
                }
                .distinctUntilChanged()
                .collect { adaptedState ->
                    _adaptedState.value = adaptedState
                    Timber.d("Updated _adaptedState: ${adaptedState?.currentStepIndex}, hasError=${adaptedState?.sendError != null}")
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
    // app/src/main/java/com/synngate/synnframe/domain/service/FsmWizardControllerAdapter.kt

    /**
     * Обрабатывает результат шага
     */
    suspend fun processStepResult(result: Any?) {
        // Очень подробное логирование для отладки
        val currentFsmState = stateMachine.currentState.value
        val currentAdaptedState = _adaptedState.value

        Timber.d("processStepResult called with: ${result?.javaClass?.simpleName}")
        Timber.d("Current FSM state: ${currentFsmState?.id}")
        Timber.d("Current adapted state: step=${currentAdaptedState?.currentStepIndex}, " +
                "results=${currentAdaptedState?.results?.entries?.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")

        // Проверяем и логируем состояние контекста машины
        val contextResults = stateMachine.wizardContext.results
        Timber.d("FSM context before processing: ${contextResults.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")

        // Если result == null, то это событие "назад"
        if (result == null) {
            Timber.d("Processing Back event")
            stateMachine.handleEvent(WizardEvent.Back)
        } else {
            Timber.d("Processing Next event with result: ${result.javaClass.simpleName}")
            // Перед обработкой результата в FSM, сохраняем его в специальные ключи
            // Это дополнительная гарантия от потери данных
            if (result is TaskProduct) {
                val context = stateMachine.wizardContext
                val updatedResults = context.results.toMutableMap()
                updatedResults["lastTaskProduct"] = result
                updatedResults["lastProduct"] = result.product
                Timber.d("Added lastTaskProduct and lastProduct to context before FSM processing")
                // Мы не можем напрямую обновить контекст машины, но можем обновить
                // промежуточное состояние адаптера
                val adaptedState = _adaptedState.value
                if (adaptedState != null) {
                    val stateResults = adaptedState.results.toMutableMap()
                    stateResults["lastTaskProduct"] = result
                    stateResults["lastProduct"] = result.product
                    val updatedState = adaptedState.copy(results = stateResults)
                    _adaptedState.value = updatedState
                    Timber.d("Updated _adaptedState with lastTaskProduct and lastProduct")
                }
            } else if (result is Product) {
                val adaptedState = _adaptedState.value
                if (adaptedState != null) {
                    val stateResults = adaptedState.results.toMutableMap()
                    stateResults["lastProduct"] = result
                    val updatedState = adaptedState.copy(results = stateResults)
                    _adaptedState.value = updatedState
                    Timber.d("Updated _adaptedState with lastProduct")
                }
            }

            // Теперь отправляем событие в машину состояний
            stateMachine.handleEvent(WizardEvent.Next(result))
        }

        // Проверяем состояние после обработки
        val newFsmState = stateMachine.currentState.value
        val newContextResults = stateMachine.wizardContext.results

        Timber.d("New FSM state after processing: ${newFsmState?.id}")
        Timber.d("New FSM context after processing: ${newContextResults.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")
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

        // Подробное логирование контекста для отладки
        Timber.d("Adapting state ${state.javaClass.simpleName} to ActionWizardState")
        Timber.d("WizardContext results: ${context.results.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")

        val result = when (state) {
            is InitializingState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = -1, // -1 означает "инициализация"
                results = context.results, // Критически важно передавать все результаты
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
                results = context.results, // Критически важно передавать все результаты
                errors = context.errors,
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = false
            )

            is CompletingState -> {
                Timber.d("Adapting CompletingState to ActionWizardState")
                // Автоматически запускаем переход в CompletedState
                adapterScope.launch {
                    Timber.d("Auto-transitioning CompletingState to CompletedState")
                    stateMachine.handleEvent(WizardEvent.Complete)
                }
                ActionWizardState(
                    taskId = context.taskId,
                    actionId = context.actionId,
                    action = context.action,
                    steps = context.steps,
                    currentStepIndex = context.steps.size, // После последнего шага
                    results = context.results, // Критически важно передавать все результаты
                    isInitialized = true,
                    lastScannedBarcode = context.lastScannedBarcode,
                    isProcessingStep = false, // Было true, меняем на false
                    isSending = false // Было true, меняем на false
                )
            }

            is CompletedState -> ActionWizardState(
                taskId = context.taskId,
                actionId = context.actionId,
                action = context.action,
                steps = context.steps,
                currentStepIndex = context.steps.size, // После последнего шага
                results = context.results, // Критически важно передавать все результаты
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
                results = context.results, // Критически важно передавать все результаты
                isInitialized = true,
                lastScannedBarcode = context.lastScannedBarcode,
                isProcessingStep = false,
                isSending = false,
                sendError = state.errorMessage
            )

            else -> null
        }

        if (result != null) {
            Timber.d("Adapted state results: ${result.results.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")
        }

        return result
    }

    override fun dispose() {
        // Отменяем все корутины при уничтожении адаптера
        stateObservationJob?.cancel()
        adapterScope.cancel()
        stateMachine.reset()
    }
}