package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import timber.log.Timber

abstract class BaseWizardState(
    override val id: String,
    protected val context: WizardContext
) : WizardState {

    override val isTerminal: Boolean = false

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // Обрабатываем общие события для всех состояний
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> null // Для других событий возвращаем null, что означает "нет перехода"
        }
    }

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
}