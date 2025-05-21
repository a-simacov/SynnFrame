package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import timber.log.Timber

/**
 * Менеджер автозаполнения полей в шагах на основе сохраненных объектов
 */
class AutoFillManager(
    private val taskContextManager: TaskContextManager
) {
    /**
     * Проверяет возможность автозаполнения для данного шага
     * @param step Шаг, для которого проверяется возможность автозаполнения
     * @param factory Фабрика, отвечающая за создание шага
     * @return true, если шаг может быть автоматически заполнен
     */
    fun canAutoFillStep(
        action: PlannedAction,
        step: ActionStep,
        factory: Any?
    ): Boolean {
        // Базовые проверки
        if (factory !is AutoCompleteCapableFactory || !factory.isAutoCompleteEnabled(step)) {
            return false
        }

        // Проверяем, входит ли шаг в шаги хранения или размещения
        val isStorageStep = action.actionTemplate.storageSteps.any { it.id == step.id }
        val isPlacementStep = action.actionTemplate.placementSteps.any { it.id == step.id }

        if (!isStorageStep && !isPlacementStep) {
            return false
        }

        // Проверяем наличие сохраненного объекта для типа данного шага
        val objectType = step.objectType
        return taskContextManager.hasSavableObjectOfType(objectType)
    }

    /**
     * Получает данные для автозаполнения шага
     * @param step Шаг, для которого запрашиваются данные
     * @return Объект для автозаполнения или null
     */
    fun <T : Any> getAutoFillData(step: ActionStep): T? {
        val objectType = step.objectType
        return taskContextManager.getSavableObjectData(objectType)
    }

    /**
     * Проверяет, должен ли шаг быть пропущен из-за автозаполнения
     * @param step Шаг, который может быть пропущен
     * @return true, если шаг может быть пропущен
     */
    fun shouldSkipStep(step: ActionStep): Boolean {
        // Обязательные шаги, которые нельзя пропустить,
        // должны отображаться даже при наличии данных для автозаполнения
        if (step.isRequired && !step.canSkip) {
            return false
        }

        // Проверяем наличие сохраненного объекта для типа данного шага
        return taskContextManager.hasSavableObjectOfType(step.objectType)
    }

    /**
     * Сохраняет объекты из результатов выполнения действия
     * @param results Результаты выполнения шагов
     * @param stepIds Идентификаторы шагов
     */
    fun saveSavableObjectsFromResults(results: Map<String, Any>, stepIds: List<String> = emptyList()) {
        val taskType = taskContextManager.lastTaskTypeX.value
        if (taskType == null || taskType.savableObjectTypes.isEmpty()) {
            return
        }

        // Проходим по всем результатам и сохраняем объекты соответствующих типов
        for ((key, value) in results) {
            val source = "step:$key"

            when (value) {
                is Pallet -> {
                    if (ActionObjectType.PALLET in taskType.savableObjectTypes) {
                        taskContextManager.addSavableObject(ActionObjectType.PALLET, value, source)
                    }
                }
                is BinX -> {
                    if (ActionObjectType.BIN in taskType.savableObjectTypes) {
                        taskContextManager.addSavableObject(ActionObjectType.BIN, value, source)
                    }
                }
                is TaskProduct -> {
                    if (ActionObjectType.TASK_PRODUCT in taskType.savableObjectTypes) {
                        taskContextManager.addSavableObject(ActionObjectType.TASK_PRODUCT, value, source)
                    }
                }
                is Product -> {
                    if (ActionObjectType.CLASSIFIER_PRODUCT in taskType.savableObjectTypes) {
                        taskContextManager.addSavableObject(ActionObjectType.CLASSIFIER_PRODUCT, value, source)
                    }
                }
            }
        }

        Timber.d("Saved objects from results: ${taskContextManager.savableObjects.value.size} objects")
    }

    /**
     * Очищает сохраненные объекты
     */
    fun clearSavableObjects() {
        taskContextManager.clearSavableObjects()
    }

    /**
     * Получает подходящие шаги для автозаполнения в контексте действия
     */
    fun getAutoFillableStepsForAction(action: PlannedAction): Set<String> {
        val taskType = taskContextManager.lastTaskTypeX.value ?: return emptySet()
        val savableObjectTypes = taskType.savableObjectTypes

        if (savableObjectTypes.isEmpty()) {
            return emptySet()
        }

        val autoFillableSteps = mutableSetOf<String>()

        // Проверяем шаги хранения
        for (step in action.actionTemplate.storageSteps) {
            if (step.objectType in savableObjectTypes &&
                taskContextManager.hasSavableObjectOfType(step.objectType)) {
                autoFillableSteps.add(step.id)
            }
        }

        // Проверяем шаги размещения
        for (step in action.actionTemplate.placementSteps) {
            if (step.objectType in savableObjectTypes &&
                taskContextManager.hasSavableObjectOfType(step.objectType)) {
                autoFillableSteps.add(step.id)
            }
        }

        return autoFillableSteps
    }
}