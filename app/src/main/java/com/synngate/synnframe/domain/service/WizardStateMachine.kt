package com.synngate.synnframe.domain.model.wizard

import timber.log.Timber

/**
 * Состояние инициализации визарда.
 * В этом состоянии загружаются данные для визарда.
 */
class InitializingState(context: WizardContext) : BaseWizardState("initializing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // При успешной инициализации переходим к первому шагу
            is WizardEvent.Initialize -> {
                // Предполагается, что данные уже загружены и помещены в context
                if (context.steps.isNotEmpty()) {
                    Timber.d("Initialization successful, moving to first step")
                    context.createStepState(0)
                } else {
                    Timber.e("No steps available after initialization")
                    context.createErrorState("Нет доступных шагов для визарда")
                }
            }
            // Обрабатываем общие события базового класса
            else -> super.handleEvent(event)
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

    val currentStep: WizardStep?
        get() = if (stepIndex < context.steps.size) context.steps[stepIndex] else null

    // Прогресс выполнения визарда (0..1)
    val progress: Float
        get() = if (context.steps.isEmpty()) 0f else stepIndex.toFloat() / context.steps.size

    // Можно ли вернуться назад
    val canGoBack: Boolean
        get() = stepIndex > 0 && (currentStep?.canNavigateBack ?: true)

    // Завершены ли все шаги
    val isCompleted: Boolean
        get() = stepIndex >= context.steps.size

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // Обработка результата текущего шага и переход к следующему
            is WizardEvent.Next -> {
                val currentStepId = currentStep?.id ?: return logUnsupportedEvent(event)

                // Сохраняем результат текущего шага
                val updatedResults = context.results + (currentStepId to event.result)
                val updatedContext = context.copy(
                    results = updatedResults,
                    lastScannedBarcode = null
                )

                // Переходим к следующему шагу
                val nextStepIndex = stepIndex + 1

                if (nextStepIndex >= context.steps.size) {
                    // Если шаги закончились, переходим к завершению визарда
                    Timber.d("All steps completed, moving to completing state")
                    updatedContext.createCompletingState()
                } else {
                    // Переходим к следующему шагу
                    Timber.d("Moving to next step: $nextStepIndex")
                    updatedContext.createStepState(nextStepIndex)
                }
            }

            // Возврат к предыдущему шагу
            is WizardEvent.Back -> {
                if (!canGoBack) {
                    return logUnsupportedEvent(event)
                }

                val previousStepIndex = stepIndex - 1
                if (previousStepIndex < 0) {
                    return logUnsupportedEvent(event)
                }

                // Очищаем результаты текущего шага при возврате
                val currentStepId = currentStep?.id
                val updatedResults = if (currentStepId != null) {
                    context.results - currentStepId
                } else {
                    context.results
                }

                val updatedContext = context.copy(
                    results = updatedResults,
                    lastScannedBarcode = null
                )

                Timber.d("Moving back to step: $previousStepIndex")
                updatedContext.createStepState(previousStepIndex)
            }

            // Обработка сканированного штрих-кода
            is WizardEvent.ProcessBarcode -> {
                if (event.barcode == context.lastScannedBarcode) {
                    Timber.d("Duplicate barcode ignored: ${event.barcode}")
                    return null
                }

                val updatedContext = context.copy(lastScannedBarcode = event.barcode)
                updatedContext.createStepState(stepIndex)
            }

            // Обработка события завершения визарда
            is WizardEvent.Complete -> {
                if (isCompleted) {
                    context.createCompletingState()
                } else {
                    logUnsupportedEvent(event)
                }
            }

            // Обрабатываем общие события базового класса
            else -> super.handleEvent(event)
        }
    }
}

/**
 * Состояние завершения визарда (отправка результатов)
 */
class CompletingState(context: WizardContext) : BaseWizardState("completing", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // Если завершение прошло успешно, переходим в конечное состояние
            is WizardEvent.Complete -> {
                Timber.d("Wizard completion successful")
                context.createCompletedState()
            }
            // При ошибке переходим в состояние ошибки
            is WizardEvent.Back -> {
                // Возвращаемся к последнему шагу
                val lastStepIndex = context.steps.size - 1
                if (lastStepIndex >= 0) {
                    context.createStepState(lastStepIndex)
                } else {
                    logUnsupportedEvent(event)
                }
            }
            // Обрабатываем общие события базового класса
            else -> super.handleEvent(event)
        }
    }
}

/**
 * Состояние успешного завершения визарда (финальное состояние)
 */
class CompletedState(context: WizardContext) : BaseWizardState("completed", context) {
    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState? {
        // В финальном состоянии поддерживаем только возврат к последнему шагу
        return when (event) {
            is WizardEvent.Back -> {
                val lastStepIndex = context.steps.size - 1
                if (lastStepIndex >= 0) {
                    context.createStepState(lastStepIndex)
                } else {
                    logUnsupportedEvent(event)
                }
            }
            // Обрабатываем общие события базового класса
            else -> super.handleEvent(event)
        }
    }
}

/**
 * Состояние отмены визарда (финальное состояние)
 */
class CancelledState(context: WizardContext) : BaseWizardState("cancelled", context) {
    override val isTerminal: Boolean = true

    override fun handleEvent(event: WizardEvent): WizardState? {
        // В отмененном состоянии не поддерживаем никаких переходов
        return logUnsupportedEvent(event)
    }
}

/**
 * Состояние ошибки визарда
 */
class ErrorState(
    val errorMessage: String,
    context: WizardContext
) : BaseWizardState("error", context) {

    override fun handleEvent(event: WizardEvent): WizardState? {
        return when (event) {
            // Из состояния ошибки можно только начать заново или отменить
            is WizardEvent.Initialize -> super.handleEvent(event)
            // Обрабатываем общие события базового класса
            else -> super.handleEvent(event)
        }
    }
}