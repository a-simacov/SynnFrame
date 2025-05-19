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
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> null
        }
    }

    protected fun logUnsupportedEvent(event: WizardEvent): WizardState? {
        return null
    }
}

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