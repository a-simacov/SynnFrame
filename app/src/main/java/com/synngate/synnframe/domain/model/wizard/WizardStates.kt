package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct

class InitializingState(context: WizardContext) : BaseWizardState("initializing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Initialize -> {
                if (context.steps.isNotEmpty()) {
                    context.createStepState(0)
                } else {
                    context.createErrorState("No steps found for action")
                }
            }
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> logUnsupportedEvent(event)
        }
    }
}

class StepState(
    val stepIndex: Int,
    context: WizardContext
) : BaseWizardState("step_$stepIndex", context) {

    val currentStep
        get() = if (stepIndex < context.steps.size) context.steps[stepIndex] else null

    val isLastStep
        get() = stepIndex >= context.steps.size - 1

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Next -> {
                val step = currentStep ?: return context.createErrorState("Invalid step index")

                val updatedResults = context.results.toMutableMap()
                updatedResults[step.id] = event.result

                // Сохраняем также специальные ключи для быстрого доступа
                when (event.result) {
                    is TaskProduct -> {
                        updatedResults["lastTaskProduct"] = event.result
                        updatedResults["lastProduct"] = event.result.product
                    }
                    is Product -> {
                        updatedResults["lastProduct"] = event.result
                    }
                    is Pallet -> {
                        updatedResults["lastPallet"] = event.result
                    }
                    is BinX -> {
                        updatedResults["lastBin"] = event.result
                    }
                }

                val updatedContext = context.copy(
                    results = updatedResults,
                    lastScannedBarcode = null
                )

                if (isLastStep) {
                    updatedContext.createCompletingState()
                } else {
                    updatedContext.createStepState(stepIndex + 1)
                }
            }
            is WizardEvent.Back -> {
                if (stepIndex > 0) {
                    context.createStepState(stepIndex - 1)
                } else {
                    this // Остаемся в текущем состоянии
                }
            }
            is WizardEvent.ProcessBarcode -> {
                val updatedContext = context.copy(lastScannedBarcode = event.barcode)
                updatedContext.createStepState(stepIndex)
            }
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> logUnsupportedEvent(event)
        }
    }
}

class CompletingState(context: WizardContext) : BaseWizardState("completing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Complete -> {
                context.createCompletedState()
            }
            is WizardEvent.Back -> {
                if (context.steps.isNotEmpty()) {
                    context.createStepState(context.steps.size - 1)
                } else {
                    context.createErrorState("No steps available to go back to")
                }
            }
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> logUnsupportedEvent(event)
        }
    }
}

class CompletedState(context: WizardContext) : BaseWizardState("completed", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState {
        return when (event) {
            is WizardEvent.Cancel -> {
                context.createCancelledState()
            }
            else -> {
                this // Остаемся в текущем состоянии
            }
        }
    }
}

class CancelledState(context: WizardContext) : BaseWizardState("cancelled", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState {
        return when (event) {
            is WizardEvent.Initialize -> {
                context.createInitializingState()
            }
            else -> {
                this // Остаемся в текущем состоянии
            }
        }
    }
}

class ErrorState(
    val errorMessage: String,
    context: WizardContext
) : BaseWizardState("error", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState {
        return when (event) {
            is WizardEvent.Initialize -> {
                context.createInitializingState()
            }
            is WizardEvent.Cancel -> {
                context.createCancelledState()
            }
            else -> {
                this // Остаемся в текущем состоянии
            }
        }
    }
}