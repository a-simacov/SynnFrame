package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import timber.log.Timber

class ActionStepExecutionService(
    private val taskXRepository: TaskXRepository,
    private val validationService: ValidationService,
    private val actionDataCacheService: ActionDataCacheService,
    private val taskContextManager: TaskContextManager // Добавляем TaskContextManager
) {

    suspend fun executeStep(
        taskId: String,
        action: PlannedAction,
        step: ActionStep,
        value: Any?,
        contextData: Map<String, Any> = emptyMap()
    ): StepExecutionResult {
        try {
            val context = buildStepContext(taskId, action, step, contextData)

            if (value == null && step.isRequired) {
                Timber.w("Step ${step.id} requires a value but received null")
                return StepExecutionResult.Error("Необходимо заполнить поле")
            }

            if (value == null && !step.canSkip) {
                Timber.w("Step ${step.id} cannot be skipped")
                return StepExecutionResult.Error("Шаг не может быть пропущен")
            }

            if (value == null && step.canSkip) {
                Timber.d("Step ${step.id} skipped")
                return StepExecutionResult.Skipped
            }

            val validationResult = validationService.validate(
                rule = step.validationRules,
                value = value,
                context = context
            )

            if (!validationResult.isSuccess) {
                val error = (validationResult as ValidationResult.Error).message
                return StepExecutionResult.Error(error)
            }

            val processedValue = processValueByObjectType(value, step.objectType)
                ?: return StepExecutionResult.Error("Неверный тип данных для этого шага")

            return StepExecutionResult.Success(
                stepId = step.id,
                value = processedValue
            )
        } catch (e: Exception) {
            Timber.e(e, "Error executing step ${step.id}")
            return StepExecutionResult.Error("Ошибка выполнения шага: ${e.message}")
        }
    }

    private suspend fun buildStepContext(
        taskId: String,
        action: PlannedAction,
        step: ActionStep,
        contextData: Map<String, Any>
    ): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // Проверяем сначала TaskContextManager
        val contextTask = taskContextManager.lastStartedTaskX.value

        // Используем задание из контекста, если оно есть и соответствует ID
        val task = if (contextTask != null && contextTask.id == taskId) {
            contextTask
        } else {
            // Иначе получаем из репозитория
            taskXRepository.getTaskById(taskId)
        }

        // Добавляем данные контекста
        context.putAll(contextData)

        // Добавляем данные задания
        task?.let {
            // Если шаг для выбора товара, добавляем список товаров из плана
            if (step.objectType == ActionObjectType.CLASSIFIER_PRODUCT ||
                step.objectType == ActionObjectType.TASK_PRODUCT) {
                val planProducts = task.plannedActions
                    .mapNotNull { it.storageProduct }
                    .toList()

                context["planItems"] = planProducts
            }

            // Если шаг для выбора паллеты, добавляем список паллет из плана
            if (step.objectType == ActionObjectType.PALLET) {
                val planPallets = task.plannedActions
                    .mapNotNull {
                        when {
                            isStorageStep(step, action) -> it.storagePallet
                            isPlacementStep(step, action) -> it.placementPallet
                            else -> null
                        }
                    }
                    .distinct()
                    .filterNotNull()
                    .toList()

                context["planItems"] = planPallets
            }

            // Если шаг для выбора ячейки, добавляем список ячеек из плана
            if (step.objectType == ActionObjectType.BIN) {
                val planBins = task.plannedActions
                    .mapNotNull {
                        when {
                            isStorageStep(step, action) -> null // Обычно ячейки хранения нет
                            isPlacementStep(step, action) -> it.placementBin
                            else -> null
                        }
                    }
                    .distinct()
                    .filterNotNull()
                    .toList()

                context["planItems"] = planBins
            }
        }

        return context
    }

    private fun isStorageStep(step: ActionStep, action: PlannedAction): Boolean {
        return action.actionTemplate.storageSteps.any { it.id == step.id }
    }

    private fun isPlacementStep(step: ActionStep, action: PlannedAction): Boolean {
        return action.actionTemplate.placementSteps.any { it.id == step.id }
    }

    private fun processValueByObjectType(value: Any?, objectType: ActionObjectType): Any? {
        return when (objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> {
                when (value) {
                    is Product -> value
                    is TaskProduct -> value.product
                    is String -> {
                        // Предполагаем, что строка - это ID товара, но в реальном приложении
                        // здесь будет поиск товара в репозитории
                        null
                    }
                    else -> null
                }
            }
            ActionObjectType.TASK_PRODUCT -> {
                when (value) {
                    is TaskProduct -> value
                    is Product -> TaskProduct(product = value)
                    else -> null
                }
            }
            ActionObjectType.PALLET -> {
                when (value) {
                    is Pallet -> value
                    is String -> {
                        // Предполагаем, что строка - это код паллеты, но в реальном приложении
                        // здесь будет поиск паллеты в репозитории
                        null
                    }
                    else -> null
                }
            }
            ActionObjectType.BIN -> {
                when (value) {
                    is BinX -> value
                    is String -> {
                        // Предполагаем, что строка - это код ячейки, но в реальном приложении
                        // здесь будет поиск ячейки в репозитории
                        null
                    }
                    else -> null
                }
            }
        }
    }
}

sealed class StepExecutionResult {

    data class Success(
        val stepId: String,
        val value: Any
    ) : StepExecutionResult()

    data class Error(val message: String) : StepExecutionResult()

    object Skipped : StepExecutionResult()
}