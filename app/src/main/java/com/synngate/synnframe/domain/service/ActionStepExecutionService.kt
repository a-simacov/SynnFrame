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

/**
 * Сервис для выполнения шагов действий
 */
class ActionStepExecutionService(
    private val taskXRepository: TaskXRepository,
    private val validationService: ValidationService,
    private val actionDataCacheService: ActionDataCacheService
) {

    /**
     * Выполняет шаг действия
     * @param taskId Идентификатор задания
     * @param action Запланированное действие
     * @param step Текущий шаг
     * @param value Значение, полученное от пользователя
     * @param contextData Данные контекста
     * @return Результат выполнения шага
     */
    suspend fun executeStep(
        taskId: String,
        action: PlannedAction,
        step: ActionStep,
        value: Any?,
        contextData: Map<String, Any> = emptyMap()
    ): StepExecutionResult {
        Timber.d("Executing step ${step.id} for action ${action.id} in task $taskId")

        try {
            // Получение контекста выполнения шага
            val context = buildStepContext(taskId, action, step, contextData)

            // Проверка обязательности шага
            if (value == null && step.isRequired) {
                Timber.w("Step ${step.id} requires a value but received null")
                return StepExecutionResult.Error("Необходимо заполнить поле")
            }

            // Проверка возможности пропуска
            if (value == null && !step.canSkip) {
                Timber.w("Step ${step.id} cannot be skipped")
                return StepExecutionResult.Error("Шаг не может быть пропущен")
            }

            // Если значение не указано, но шаг можно пропустить
            if (value == null && step.canSkip) {
                Timber.d("Step ${step.id} skipped")
                return StepExecutionResult.Skipped
            }

            // Валидация значения по правилам
            val validationResult = validationService.validate(
                rule = step.validationRules,
                value = value,
                context = context
            )

            // Обработка результата валидации
            if (!validationResult.isSuccess) {
                val error = (validationResult as ValidationResult.Error).message
                Timber.w("Step ${step.id} validation failed: $error")
                return StepExecutionResult.Error(error)
            }

            // Преобразование и проверка значения в зависимости от типа объекта
            val processedValue = processValueByObjectType(value, step.objectType)
                ?: return StepExecutionResult.Error("Неверный тип данных для этого шага")

            Timber.d("Step ${step.id} executed successfully")
            return StepExecutionResult.Success(
                stepId = step.id,
                value = processedValue
            )
        } catch (e: Exception) {
            Timber.e(e, "Error executing step ${step.id}")
            return StepExecutionResult.Error("Ошибка выполнения шага: ${e.message}")
        }
    }

    /**
     * Создает контекст для выполнения шага
     */
    private suspend fun buildStepContext(
        taskId: String,
        action: PlannedAction,
        step: ActionStep,
        contextData: Map<String, Any>
    ): Map<String, Any> {
        val task = taskXRepository.getTaskById(taskId)
        val context = mutableMapOf<String, Any>()

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

    /**
     * Определяет, является ли шаг частью шагов хранения
     */
    private fun isStorageStep(step: ActionStep, action: PlannedAction): Boolean {
        return action.actionTemplate.storageSteps.any { it.id == step.id }
    }

    /**
     * Определяет, является ли шаг частью шагов размещения
     */
    private fun isPlacementStep(step: ActionStep, action: PlannedAction): Boolean {
        return action.actionTemplate.placementSteps.any { it.id == step.id }
    }

    /**
     * Обрабатывает значение в зависимости от типа объекта
     */
    private fun processValueByObjectType(value: Any?, objectType: ActionObjectType): Any? {
        return when (objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> {
                when (value) {
                    is Product -> value
                    is TaskProduct -> value.product
                    is String -> {
                        // Предполагаем, что строка - это ID товара, но в реальном приложении
                        // здесь будет поиск товара в репозитории
                        Timber.w("String value for Product not supported in this implementation")
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
                        Timber.w("String value for Pallet not supported in this implementation")
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
                        Timber.w("String value for BinX not supported in this implementation")
                        null
                    }
                    else -> null
                }
            }
        }
    }
}

/**
 * Результат выполнения шага
 */
sealed class StepExecutionResult {
    /**
     * Успешное выполнение шага
     */
    data class Success(
        val stepId: String,
        val value: Any
    ) : StepExecutionResult()

    /**
     * Ошибка при выполнении шага
     */
    data class Error(val message: String) : StepExecutionResult()

    /**
     * Шаг пропущен
     */
    object Skipped : StepExecutionResult()
}