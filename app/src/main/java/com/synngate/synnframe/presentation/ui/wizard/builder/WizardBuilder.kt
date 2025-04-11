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

        // Перебираем все группы действий в конфигурации
        taskType.factLineActionGroups.forEach { actionGroup ->
            Timber.d("Создание шагов для группы: ${actionGroup.name}")

            // Создаем шаг для установки WMS-действия для текущей группы
            steps.add(WizardStep(
                id = "WMS_ACTION_${actionGroup.id}",
                title = "Установка действия: ${actionGroup.name}",
                content = { context ->
                    // Автоматически устанавливаем WMS-действие группы и переходим дальше
                    context.onComplete(actionGroup.wmsAction)
                }
            ))

            // Создаем шаги для каждого действия в группе
            actionGroup.actions
                .sortedBy { it.order }
                .forEach { action ->
                    // Находим фабрику компонентов для типа действия
                    val componentFactory = stepComponentRegistry[action.actionType]
                        ?: run {
                            Timber.e("Не найдена фабрика для типа действия: ${action.actionType}")
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

        Timber.d("Всего создано шагов: ${steps.size}")
        return steps
    }
}