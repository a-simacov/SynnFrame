package com.synngate.synnframe.domain.model.wizard

import timber.log.Timber

/**
 * Состояние инициализации визарда.
 * В этом состоянии загружаются данные для визарда.
 */
class InitializingState(context: WizardContext) : BaseWizardState("initializing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Initialize -> {
                Timber.d("Initializing wizard for task ${event.taskId}, action ${event.actionId}")
                // Здесь должна быть логика инициализации
                // После успешной инициализации переходим к первому шагу
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

/**
 * Состояние выполнения конкретного шага визарда.
 */
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

                // Сохраняем результат текущего шага
                val updatedResults = context.results.toMutableMap()
                updatedResults[step.id] = event.result

                // Создаем обновленный контекст с новыми результатами
                val updatedContext = context.copy(
                    results = updatedResults,
                    lastScannedBarcode = null
                )

                // Определяем, нужно ли перейти к следующему шагу или завершить визард
                if (isLastStep) {
                    Timber.d("Last step completed, moving to completing state")
                    updatedContext.createCompletingState()
                } else {
                    Timber.d("Moving to next step ${stepIndex + 1}")
                    updatedContext.createStepState(stepIndex + 1)
                }
            }
            is WizardEvent.Back -> {
                if (stepIndex > 0) {
                    Timber.d("Moving back to step ${stepIndex - 1}")
                    context.createStepState(stepIndex - 1)
                } else {
                    Timber.d("Cannot go back from first step")
                    this // Остаемся в текущем состоянии
                }
            }
            is WizardEvent.ProcessBarcode -> {
                // Обновляем контекст с новым штрих-кодом
                val updatedContext = context.copy(lastScannedBarcode = event.barcode)
                // Создаем новое состояние с тем же шагом, но обновленным контекстом
                updatedContext.createStepState(stepIndex)
            }
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> logUnsupportedEvent(event)
        }
    }
}

/**
 * Состояние завершения визарда (отправка результатов)
 */
class CompletingState(context: WizardContext) : BaseWizardState("completing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Complete -> {
                Timber.d("Completing wizard")
                // Здесь должна быть логика завершения визарда
                // После успешного завершения переходим в состояние "завершено"
                context.createCompletedState()
            }
            is WizardEvent.Back -> {
                Timber.d("Going back from completing state")
                // Возвращаемся к последнему шагу
                if (context.steps.isNotEmpty()) {
                    context.createStepState(context.steps.size - 1)
                } else {
                    // Если шагов нет, переходим в состояние ошибки
                    context.createErrorState("No steps available to go back to")
                }
            }
            is WizardEvent.Cancel -> context.createCancelledState()
            else -> logUnsupportedEvent(event)
        }
    }
}

/**
 * Состояние успешного завершения визарда (финальное состояние)
 */
class CompletedState(context: WizardContext) : BaseWizardState("completed", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState? {
        // Это терминальное состояние, поэтому большинство событий не обрабатываются
        return when (event) {
            is WizardEvent.Cancel -> {
                Timber.d("Cancelling from completed state")
                context.createCancelledState()
            }
            else -> {
                Timber.d("Event $event ignored in completed state")
                this // Остаемся в текущем состоянии
            }
        }
    }
}

/**
 * Состояние отмены визарда (финальное состояние)
 */
class CancelledState(context: WizardContext) : BaseWizardState("cancelled", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState? {
        // Это терминальное состояние, поэтому все события игнорируются,
        // кроме возможности новой инициализации
        return when (event) {
            is WizardEvent.Initialize -> {
                Timber.d("Reinitializing wizard from cancelled state")
                // Начинаем новую инициализацию
                context.createInitializingState()
            }
            else -> {
                Timber.d("Event $event ignored in cancelled state")
                this // Остаемся в текущем состоянии
            }
        }
    }
}

/**
 * Состояние ошибки визарда
 */
class ErrorState(
    val errorMessage: String,
    context: WizardContext
) : BaseWizardState("error", context) {

    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            is WizardEvent.Initialize -> {
                Timber.d("Reinitializing wizard from error state")
                // Повторная инициализация
                context.createInitializingState()
            }
            is WizardEvent.Cancel -> {
                Timber.d("Cancelling from error state")
                context.createCancelledState()
            }
            else -> {
                Timber.d("Event $event ignored in error state")
                this // Остаемся в текущем состоянии
            }
        }
    }
}