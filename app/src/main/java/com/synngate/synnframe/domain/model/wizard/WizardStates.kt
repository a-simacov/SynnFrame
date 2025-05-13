package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
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

                // Подробное логирование сохраняемого результата
                Timber.d("Saving result for step ${step.id}: ${event.result} (${event.result.javaClass.simpleName})")
                updatedResults[step.id] = event.result

                // Сохраняем также специальные ключи для быстрого доступа
                when (event.result) {
                    is TaskProduct -> {
                        updatedResults["lastTaskProduct"] = event.result
                        Timber.d("Also saved as lastTaskProduct for easier retrieval")

                        // Также сохраняем продукт отдельно
                        updatedResults["lastProduct"] = event.result.product
                        Timber.d("Also saved as lastProduct for easier retrieval")
                    }
                    is Product -> {
                        updatedResults["lastProduct"] = event.result
                        Timber.d("Also saved as lastProduct for easier retrieval")
                    }
                    is Pallet -> {
                        updatedResults["lastPallet"] = event.result
                        Timber.d("Also saved as lastPallet for easier retrieval")
                    }
                    is BinX -> {
                        updatedResults["lastBin"] = event.result
                        Timber.d("Also saved as lastBin for easier retrieval")
                    }
                }

                // Создаем обновленный контекст с новыми результатами
                val updatedContext = context.copy(
                    results = updatedResults,
                    lastScannedBarcode = null
                )

                // Детальное логирование для отладки
                Timber.d("Updated context results: ${updatedContext.results.entries.joinToString { "${it.key} -> ${it.value?.javaClass?.simpleName}" }}")

                // Добавляем проверку для специфических случаев с TaskProduct
                if (event.result is TaskProduct) {
                    val next = stepIndex + 1
                    if (next < context.steps.size) {
                        val nextStep = context.steps[next]
                        Timber.d("Next step will be: ${nextStep.id}, checking if it needs TaskProduct...")

                        // Если следующий шаг идентифицирован как шаг ввода количества товара,
                        // проверяем, что мы действительно передали TaskProduct
                        if (nextStep.title.contains("количество", ignoreCase = true) ||
                            nextStep.id.contains("quantity", ignoreCase = true)) {
                            Timber.d("Next step appears to be a quantity step, ensuring TaskProduct is in context")

                            // Дополнительное логирование для проверки
                            if ("lastTaskProduct" in updatedResults) {
                                Timber.d("lastTaskProduct is in context")
                            } else {
                                Timber.w("lastTaskProduct is NOT in context, this may cause problems!")
                            }
                        }
                    }
                }

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