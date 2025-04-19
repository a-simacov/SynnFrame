package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.WizardStep
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Контроллер для управления визардом действий
 */
class ActionWizardController(
    private val taskXRepository: TaskXRepository,
    private val actionExecutionService: ActionExecutionService,
    private val actionStepExecutionService: ActionStepExecutionService
) {
    private val _wizardState = MutableStateFlow<ActionWizardState?>(null)
    val wizardState: StateFlow<ActionWizardState?> = _wizardState.asStateFlow()

    /**
     * Инициализирует визард для выполнения действия
     * @param taskId Идентификатор задания
     * @param actionId Идентификатор действия
     * @return Результат инициализации
     */
    suspend fun initialize(taskId: String, actionId: String): Result<Boolean> {
        return try {
            Timber.d("Initializing wizard for task $taskId, action $actionId")
            // Сначала сбрасываем текущее состояние
            _wizardState.value = null

            // Получаем задание и запланированное действие
            val task = taskXRepository.getTaskById(taskId)
                ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))

            val action = task.plannedActions.find { it.id == actionId }
                ?: return Result.failure(IllegalArgumentException("Action not found: $actionId"))

            // Создаем шаги для визарда на основе шаблона действия
            val steps = createStepsFromAction(action)

            if (steps.isEmpty()) {
                Timber.w("No steps created for action ${action.id}")
                return Result.failure(IllegalStateException("No steps created for this action"))
            }

            // Создаем начальное состояние
            _wizardState.value = ActionWizardState(
                taskId = taskId,
                actionId = actionId,
                action = action,
                steps = steps,
                results = mapOf(),
                startedAt = LocalDateTime.now(),
                isInitialized = true,
                lastScannedBarcode = null
            )

            Timber.d("Wizard initialized successfully with ${steps.size} steps")
            Result.success(true)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing wizard: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Обрабатывает штрихкод от сканера
     * @param barcode Отсканированный штрихкод
     */
    suspend fun processBarcodeFromScanner(barcode: String) {
        val currentState = _wizardState.value ?: return

        try {
            Timber.d("Processing barcode from scanner: $barcode")

            // Обновляем состояние с новым штрихкодом
            _wizardState.value = currentState.copy(
                lastScannedBarcode = barcode
            )

            // Текущий шаг
            val currentStep = currentState.currentStep ?: return
            val stepId = currentStep.id

            // Если это последний экран, просто сохраняем штрихкод без дополнительной обработки
            if (currentState.isCompleted) {
                return
            }

            // Найдем ActionStep для текущего шага, чтобы определить тип обрабатываемого объекта
            val action = currentState.action ?: return
            val actionStep = findActionStepForWizardStep(action, stepId)

            if (actionStep != null) {
                // Пытаемся автоматически обработать штрихкод в зависимости от типа объекта
                // Для простоты, мы только сохраняем штрихкод в состоянии
                // Каждый компонент шага будет сам решать, как использовать этот штрихкод

                Timber.d("Barcode ${barcode} saved for step ${stepId}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing barcode: ${e.message}")
        }
    }

    /**
     * Находит ActionStep для указанного ID шага визарда
     */
    private fun findActionStepForWizardStep(action: PlannedAction, stepId: String): ActionStep? {
        // Ищем в шагах хранения
        action.actionTemplate.storageSteps.find { it.id == stepId }?.let {
            return it
        }

        // Ищем в шагах размещения
        action.actionTemplate.placementSteps.find { it.id == stepId }?.let {
            return it
        }

        return null
    }

    /**
     * Создает шаги визарда на основе шаблона действия
     */
    private fun createStepsFromAction(action: PlannedAction): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()
        val template = action.actionTemplate

        Timber.d("Creating steps from action template: ${template.name}")

        // Проверяем наличие шагов в шаблоне
        if (template.storageSteps.isEmpty() && template.placementSteps.isEmpty()) {
            Timber.w("Action template has no steps: ${template.id}")
            return emptyList()
        }

        // Добавляем шаги для объекта хранения
        template.storageSteps.sortedBy { it.order }.forEach { actionStep ->
            Timber.d("Adding storage step: ${actionStep.id}")
            steps.add(createWizardStep(actionStep))
        }

        // Добавляем шаги для объекта размещения
        template.placementSteps.sortedBy { it.order }.forEach { actionStep ->
            Timber.d("Adding placement step: ${actionStep.id}")
            steps.add(createWizardStep(actionStep))
        }

        return steps
    }

    /**
     * Создает шаг визарда на основе шага действия
     * Не заполняет содержимое, это будет сделано в UI
     */
    private fun createWizardStep(actionStep: ActionStep): WizardStep {
        return WizardStep(
            id = actionStep.id,
            title = actionStep.name,
            // Пустая функция для содержимого, будет заполнена в UI
            content = { _ -> /* Заполняется в ActionWizardScreen */ },
            canNavigateBack = true,
            isAutoComplete = false,
            shouldShow = { true }
        )
    }

    /**
     * Обрабатывает результат выполнения шага
     * @param result Результат выполнения шага или null для шага назад
     */
    suspend fun processStepResult(result: Any?) {
        val state = _wizardState.value ?: return

        try {
            // Если результат null, это означает шаг назад
            if (result == null) {
                handleBackStep(state)
                return
            }

            // Обработка результата шага
            val currentStep = state.currentStep
            if (currentStep != null && !state.isCompleted) {
                // Обновляем результаты
                val updatedResults = state.results.toMutableMap()
                updatedResults[currentStep.id] = result

                // Переходим к следующему шагу
                _wizardState.value = state.copy(
                    currentStepIndex = state.currentStepIndex + 1,
                    results = updatedResults
                )

                Timber.d("Step completed, moving to next step (${state.currentStepIndex + 1})")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing step result")
        }
    }

    /**
     * Обрабатывает переход вперед без изменения результата
     * Используется для перехода вперед, когда шаг уже был выполнен
     */
    suspend fun processForwardStep() {
        val state = _wizardState.value ?: return
        handleForwardStep(state)
    }

    /**
     * Обрабатывает шаг вперед - переходит к следующему шагу без сохранения нового результата
     */
    private fun handleForwardStep(state: ActionWizardState) {
        // Проверяем, что мы не на итоговом экране и текущий шаг выполнен (имеет результат)
        val currentStep = state.currentStep
        if (!state.isCompleted && currentStep != null && state.results.containsKey(currentStep.id)) {
            // Переходим к следующему шагу
            _wizardState.value = state.copy(
                currentStepIndex = state.currentStepIndex + 1
            )
            Timber.d("Moving forward to next step without changing result")
        }
    }

    /**
     * Обрабатывает шаг назад
     */
    private fun handleBackStep(state: ActionWizardState) {
        if (state.isCompleted) {
            // Возврат из итогового экрана к последнему шагу
            if (state.steps.isNotEmpty()) {
                _wizardState.value = state.copy(
                    currentStepIndex = state.steps.size - 1
                )
                Timber.d("Returning from summary to last step")
            }
        } else if (state.canGoBack && state.currentStepIndex > 0) {
            // Возврат к предыдущему шагу (только если есть предыдущий шаг)
            _wizardState.value = state.copy(
                currentStepIndex = state.currentStepIndex - 1
            )
            Timber.d("Going back to previous step")
        } else {
            Timber.d("Cannot go back from first step or step doesn't allow back navigation")
        }
    }

    /**
     * Отменяет выполнение визарда
     */
    fun cancel() {
        Timber.d("Cancelling wizard")
        _wizardState.value = null
    }

    /**
     * Завершает выполнение действия и создает фактическое действие
     * @return Результат с обновленным заданием
     */
    suspend fun complete(): Result<TaskX> {
        val state = _wizardState.value
            ?: return Result.failure(IllegalStateException("Wizard is not initialized"))

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Wizard is not completed"))
        }

        try {
            Timber.d("Completing action ${state.actionId} for task ${state.taskId}")

            // Выполняем действие через сервис
            val result = actionExecutionService.executeAction(
                state.taskId,
                state.actionId,
                state.results
            )

            // Сбрасываем состояние визарда
            _wizardState.value = null

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error completing action")
            return Result.failure(e)
        }
    }
}