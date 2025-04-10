package com.synngate.synnframe.presentation.ui.wizard.builder

import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.model.wizard.WizardStep
import com.synngate.synnframe.presentation.ui.wizard.component.StepComponentFactory
import timber.log.Timber

/**
 * Билдер для создания шагов мастера
 */
class WizardBuilder {
    // Регистр доступных компонентов шагов
    private val stepComponentRegistry = mutableMapOf<FactLineXActionType, StepComponentFactory>()

    /**
     * Регистрация фабрики компонентов для определенного типа шага
     */
    fun registerStepComponent(actionType: FactLineXActionType, factory: StepComponentFactory) {
        stepComponentRegistry[actionType] = factory
    }

    /**
     * Построение шагов для задания на основе конфигурации его типа
     */
    suspend fun buildSteps(taskType: TaskTypeX): List<WizardStep> {
        val steps = mutableListOf<WizardStep>()

        taskType.factLineActionGroups.forEach { actionGroup ->
            Timber.d("Steps creation for the group: ${actionGroup.name}")

            actionGroup.actions
                .sortedBy { it.order }
                .forEach { action ->
                    // Находим фабрику компонентов для типа действия
                    val componentFactory = stepComponentRegistry[action.actionType]
                        ?: run {
                            Timber.e("Not found factory for the action type: ${action.actionType}")
                            return@forEach
                        }

                    // Создаем шаг с компонентом из фабрики
                    steps.add(WizardStep(
                        id = "step_${actionGroup.id}_${action.id}",
                        title = action.name,
                        action = action,
                        content = { context ->
                            // Используем фабрику для создания компонента шага
                            componentFactory.createComponent(
                                action = action,
                                groupContext = actionGroup,
                                wizardContext = context
                            )
                        },
                        // Валидация результата
                        validator = { results ->
                            componentFactory.validateStepResult(action, results)
                        }
                    ))
                }
        }

        Timber.d("All steps created: ${steps.size}")
        return steps
    }
}