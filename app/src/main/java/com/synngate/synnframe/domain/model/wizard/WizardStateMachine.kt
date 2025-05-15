package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardLogger
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Улучшенная машина состояний для управления визардом действий.
 * Предоставляет единый интерфейс для работы с визардом без необходимости адаптеров.
 * Все методы безопасны в отношении потоков и исключений.
 */
class WizardStateMachine(
    private val taskContextManager: TaskContextManager,
    private val actionExecutionService: ActionExecutionService
) : Disposable {
    private val TAG = "WizardStateMachine"

    // Состояние визарда (доступное для UI)
    private val _state = MutableStateFlow<ActionWizardState?>(null)
    val state: StateFlow<ActionWizardState?> = _state.asStateFlow()

    // Сохраненное состояние для восстановления после ошибок
    private var _lastSavedState: ActionWizardState? = null

    // Флаг, указывающий, был ли визард явно отменен
    private var isExplicitlyCancelled = false

    /**
     * Инициализирует визард для указанного задания и действия
     * @return результат инициализации
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        Timber.d("$TAG: Initializing wizard for task $taskId, action $actionId")
        isExplicitlyCancelled = false

        try {
            // Получаем задание из контекста
            val task = taskContextManager.lastStartedTaskX.value
                ?: return Result.failure(IllegalStateException("Task not found in context"))

            if (task.id != taskId) {
                return Result.failure(IllegalStateException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            // Находим действие в задании
            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalStateException("Action not found: $actionId"))

            // Получаем тип задания
            val taskType = taskContextManager.lastTaskTypeX.value
                ?: return Result.failure(IllegalStateException("Task type not found in context"))

            // Создаем шаги из шаблона действия
            val steps = createStepsFromAction(action)
            if (steps.isEmpty()) {
                return Result.failure(IllegalStateException("No steps found for action"))
            }

            // Собираем имеющиеся факт-действия для контекста
            val factActionsMap = task.factActions
                .groupBy { it.plannedActionId }
                .mapValues { (_, factActions) -> factActions }

            // Создаем базовое состояние визарда с обогащенными данными
            val initialData = mutableMapOf<String, Any>(
                "taskType" to taskType,
                "factActions" to factActionsMap
            )

            // Если в действии есть Product/TaskProduct, добавляем его в контекст
            action.storageProduct?.let {
                initialData["actionTaskProduct"] = it
                initialData["actionProduct"] = it.product

                // Добавляем и как специальные ключи
                initialData["lastTaskProduct"] = it
                initialData["lastProduct"] = it.product
            }

            val initialState = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                currentStepIndex = 0,
                results = initialData,
                startedAt = LocalDateTime.now(),
                isInitialized = true
            )

            // Сохраняем состояние для возможного восстановления
            saveStateForRecovery(initialState)

            // Устанавливаем начальное состояние - используем update вместо прямой установки значения
            _state.update { initialState }

            return Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error initializing wizard")
            return Result.failure(e)
        }
    }

    /**
     * Обрабатывает результат шага
     * @param result результат шага или null для навигации назад
     */
    suspend fun processStepResult(result: Any?) {
        // Проверяем, не был ли визард отменен
        if (isExplicitlyCancelled) {
            Timber.d("$TAG: Ignoring step result as wizard was explicitly cancelled")
            return
        }

        val currentState = _state.value ?: return

        try {
            if (result == null) {
                // Навигация назад
                navigateBack(currentState)
            } else {
                // Сохраняем текущее состояние перед изменением
                saveStateForRecovery(currentState)

                // Сохраняем результат и переходим к следующему шагу
                saveStepResultAndMoveForward(currentState, result)
            }
        } catch (e: Exception) {
            WizardLogger.logError(TAG, e, "processing step result")

            // Пытаемся восстановить предыдущее состояние
            val recoveredState = recoverFromError(e.message ?: "Unknown error")
            if (recoveredState != null) {
                _state.update { recoveredState }
            } else {
                // Обновляем состояние с ошибкой, если не смогли восстановить
                _state.update { state ->
                    state?.let {
                        val updatedErrors = it.errors.toMutableMap()
                        updatedErrors[it.currentStep?.id ?: ""] = e.message ?: "Unknown error"
                        it.copy(errors = updatedErrors)
                    }
                }
            }
        }
    }

    /**
     * Обрабатывает штрих-код от сканера
     */
    fun processBarcodeFromScanner(barcode: String) {
        // Проверка, не был ли визард отменен
        if (isExplicitlyCancelled) {
            Timber.d("$TAG: Ignoring barcode as wizard was explicitly cancelled")
            return
        }

        Timber.d("$TAG: Обработка штрих-кода: $barcode")
        _state.update { state ->
            state?.copy(lastScannedBarcode = barcode)
        }
    }

    /**
     * Отменяет визард
     */
    fun cancel() {
        Timber.d("$TAG: Wizard cancelled explicitly")
        // Устанавливаем флаг явной отмены
        isExplicitlyCancelled = true
        reset()
    }

    /**
     * Завершает визард с выполнением действия
     */
    suspend fun complete(): Result<TaskX> {
        // Проверка, не был ли визард отменен
        if (isExplicitlyCancelled) {
            Timber.d("$TAG: Ignoring complete request as wizard was explicitly cancelled")
            return Result.failure(IllegalStateException("Wizard was cancelled"))
        }

        try {
            // Сохраняем текущее состояние перед выполнением действия
            val currentState = _state.value ?:
            return Result.failure(IllegalStateException("Wizard not initialized"))

            saveStateForRecovery(currentState)

            // Устанавливаем флаг отправки
            _state.update { it?.copy(isSending = true) }

            // Улучшенная обработка данных перед отправкой
            val enrichedResults = WizardUtils.enrichResultsData(currentState.results)

            // Выполняем действие через ActionExecutionService
            val result = actionExecutionService.executeAction(
                taskId = currentState.taskId,
                actionId = currentState.actionId,
                stepResults = enrichedResults,
                completeAction = true
            )

            // Обновляем состояние в зависимости от результата
            if (result.isSuccess) {
                _state.update { it?.copy(isSending = false, sendError = null) }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"

                // Пытаемся восстановить состояние после ошибки
                val recoveredState = recoverFromError(errorMessage)
                if (recoveredState != null) {
                    _state.update { recoveredState }
                } else {
                    _state.update { it?.copy(isSending = false, sendError = errorMessage) }
                }
            }

            return result
        } catch (e: Exception) {
            WizardLogger.logError(TAG, e, "completing wizard")

            // Пытаемся восстановить состояние после ошибки
            val recoveredState = recoverFromError(e.message ?: "Unknown error")
            if (recoveredState != null) {
                _state.update { recoveredState }
            } else {
                _state.update { it?.copy(isSending = false, sendError = e.message) }
            }

            return Result.failure(e)
        }
    }

    /**
     * Сбрасывает машину состояний
     */
    override fun dispose() {
        reset()
    }

    /**
     * Сбрасывает состояние машины
     */
    fun reset() {
        Timber.d("$TAG: Resetting wizard state machine")
        // Важно использовать update вместо прямой установки, чтобы сохранить атомарность
        _state.update { null }
        _lastSavedState = null
    }

    /**
     * Сохраняет текущее состояние для возможного восстановления
     */
    private fun saveStateForRecovery(state: ActionWizardState) {
        _lastSavedState = state.copy()
        Timber.d("$TAG: Saved state for recovery: step=${state.currentStepIndex}")
    }

    /**
     * Восстанавливает состояние после ошибки
     */
    private fun recoverFromError(errorMessage: String): ActionWizardState? {
        val savedState = _lastSavedState ?: return null

        Timber.d("$TAG: Recovering from error: $errorMessage")

        // Создаем новое состояние на основе сохраненного, но с установленной ошибкой
        return savedState.copy(
            sendError = errorMessage,
            isSending = false
        )
    }

    /**
     * Навигация назад
     */
    private fun navigateBack(currentState: ActionWizardState) {
        if (currentState.currentStepIndex > 0) {
            // Если мы не на первом шаге, переходим к предыдущему
            _state.update { it?.copy(
                currentStepIndex = currentState.currentStepIndex - 1,
                lastScannedBarcode = null
            ) }
        } else if (currentState.isCompleted) {
            // Если мы на экране завершения, возвращаемся к последнему шагу
            _state.update { it?.copy(
                currentStepIndex = currentState.steps.size - 1,
                lastScannedBarcode = null
            ) }
        }
    }

    /**
     * Сохраняет результат шага и переходит к следующему
     */
    private fun saveStepResultAndMoveForward(currentState: ActionWizardState, result: Any) {
        val currentStep = currentState.currentStep ?: return
        val updatedResults = currentState.results.toMutableMap()

        // Сохраняем результат текущего шага
        updatedResults[currentStep.id] = result

        // Логируем сохраняемый результат
        Timber.d("Saving result for step ${currentStep.id}: ${result.javaClass.simpleName}")

        // Также сохраняем специальные ключи для быстрого доступа
        when (result) {
            is TaskProduct -> {
                // Проверяем, есть ли уже TaskProduct для этого продукта
                val existingTaskProduct = updatedResults["lastTaskProduct"] as? TaskProduct

                if (existingTaskProduct == null ||
                    existingTaskProduct.product.id != result.product.id ||
                    existingTaskProduct.quantity != result.quantity) {

                    // Сохраняем новый TaskProduct
                    updatedResults["lastTaskProduct"] = result
                    Timber.d("Updated lastTaskProduct with quantity ${result.quantity}")

                    // Также сохраняем продукт отдельно
                    updatedResults["lastProduct"] = result.product
                }
            }
            is Product -> {
                updatedResults["lastProduct"] = result
                Timber.d("Updated lastProduct: ${result.name}")
            }
            is Pallet -> {
                updatedResults["lastPallet"] = result
                Timber.d("Updated lastPallet: ${result.code}")
            }
            is BinX -> {
                updatedResults["lastBin"] = result
                Timber.d("Updated lastBin: ${result.code}")
            }
        }

        // Определяем следующий шаг
        val nextStepIndex = if (currentState.currentStepIndex < currentState.steps.size - 1) {
            currentState.currentStepIndex + 1
        } else {
            currentState.steps.size // Указывает на завершение визарда
        }

        // Обновляем состояние используя update для атомарного обновления
        _state.update { state ->
            state?.copy(
                currentStepIndex = nextStepIndex,
                results = updatedResults,
                lastScannedBarcode = null
            )
        }

        // При создании итогового экрана логируем все результаты
        if (nextStepIndex >= currentState.steps.size) {
            Timber.d("Creating summary screen, all results:")
            updatedResults.entries.forEach { (key, value) ->
                when (value) {
                    is TaskProduct -> Timber.d("  $key: TaskProduct(${value.product.name}, quantity=${value.quantity})")
                    is Product -> Timber.d("  $key: Product(${value.name})")
                    is Pallet -> Timber.d("  $key: Pallet(${value.code})")
                    is BinX -> Timber.d("  $key: BinX(${value.code})")
                    else -> Timber.d("  $key: ${value.javaClass.simpleName}")
                }
            }
        }
    }

    /**
     * Создает шаги из шаблона действия
     */
    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

        // Добавляем шаги хранения
        template.storageSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(
                WizardStep(
                    id = actionStep.id,
                    title = actionStep.name,
                    content = { /* Заполняется в UI */ },
                    canNavigateBack = true,
                    isAutoComplete = false,
                    shouldShow = { true }
                )
            )
        }

        // Добавляем шаги размещения
        template.placementSteps.sortedBy { it.order }.forEach { actionStep ->
            steps.add(
                WizardStep(
                    id = actionStep.id,
                    title = actionStep.name,
                    content = { /* Заполняется в UI */ },
                    canNavigateBack = true,
                    isAutoComplete = false,
                    shouldShow = { true }
                )
            )
        }

        return steps
    }
}