package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import timber.log.Timber

/**
 * Базовый абстрактный класс для состояний визарда.
 * Предоставляет общую функциональность для всех состояний.
 */
abstract class BaseWizardState(
    override val id: String,
    protected val context: WizardContext
) : WizardState {

    override val isTerminal: Boolean = false

    /**
     * Базовая реализация обработки события.
     * Подклассы должны переопределить этот метод для специфической логики.
     */
    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // Обрабатываем общие события для всех состояний
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> null // Для других событий возвращаем null, что означает "нет перехода"
        }
    }

    /**
     * Логирует попытку обработать неподдерживаемое событие
     */
    protected fun logUnsupportedEvent(event: WizardEvent): WizardState? {
        Timber.w("Event $event is not supported in state $id")
        return null
    }
}

/**
 * Контекст визарда, содержащий общие данные для всех состояний.
 * Также отвечает за создание новых состояний.
 */
data class WizardContext(
    val taskId: String = "",
    val actionId: String = "",
    val task: TaskX? = null,
    val action: PlannedAction? = null,
    val steps: List<WizardStep> = emptyList(),
    val results: Map<String, Any> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val lastScannedBarcode: String? = null
) {
    // Фабричные методы для создания состояний

    fun createInitializingState(): InitializingState {
        return InitializingState(this)
    }

    fun createStepState(stepIndex: Int): StepState {
        return StepState(stepIndex, this)
    }

    fun createCompletingState(): CompletingState {
        return CompletingState(this)
    }

    fun createCompletedState(): CompletedState {
        return CompletedState(this)
    }

    fun createCancelledState(): CancelledState {
        return CancelledState(this)
    }

    fun createErrorState(error: String): ErrorState {
        return ErrorState(error, this)
    }

    /**
     * Создает новый контекст с обновленными данными
     */
//    fun copy(
//        taskId: String = this.taskId,
//        actionId: String = this.actionId,
//        task: TaskX? = this.task,
//        action: PlannedAction? = this.action,
//        steps: List<WizardStep> = this.steps,
//        results: Map<String, Any> = this.results,
//        errors: Map<String, String> = this.errors,
//        lastScannedBarcode: String? = this.lastScannedBarcode
//    ): WizardContext {
//        return WizardContext(
//            taskId = taskId,
//            actionId = actionId,
//            task = task,
//            action = action,
//            steps = steps,
//            results = results,
//            errors = errors,
//            lastScannedBarcode = lastScannedBarcode
//        )
//    }
}