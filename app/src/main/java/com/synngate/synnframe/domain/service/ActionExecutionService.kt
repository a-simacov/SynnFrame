package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Сервис для выполнения действий задания
 * Упрощенная версия, работающая только с локальными данными и TaskContextManager
 */
class ActionExecutionService(
    private val validationService: ValidationService,
    private val taskContextManager: TaskContextManager
) {
    suspend fun executeAction(
        taskId: String,
        actionId: String,
        stepResults: Map<String, Any>
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            // Получаем задание только из контекста
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Создаем фактическое действие на основе введенных данных
            val factAction = createFactAction(taskId, action, stepResults)

            // Создаем обновленное задание с новым фактическим действием
            // В реальности здесь должен быть вызов API для сохранения факта на сервере,
            // но мы ограничимся локальным обновлением для примера
            val factActions = task.factActions.toMutableList()
            factActions.add(factAction)

            // Обновляем запланированное действие, помечая его как выполненное
            val updatedPlannedActions = task.plannedActions.map {
                if (it.id == actionId) it.copy(isCompleted = true) else it
            }

            // Создаем обновленное задание
            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                factActions = factActions,
                lastModifiedAt = LocalDateTime.now()
            )

            // В полной реализации здесь был бы вызов API для сохранения
            // Вместо этого мы просто обновляем данные в контексте
            taskContextManager.updateTask(updatedTask)

            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error executing action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    suspend fun skipAction(
        taskId: String,
        actionId: String
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            // Обновляем запланированное действие, помечая его как пропущенное
            val updatedPlannedActions = task.plannedActions.map {
                if (it.id == actionId) it.copy(isSkipped = true) else it
            }

            // Создаем обновленное задание
            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                lastModifiedAt = LocalDateTime.now()
            )

            // Обновляем данные в контексте
            taskContextManager.updateTask(updatedTask)

            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error skipping action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    private fun createFactAction(
        taskId: String,
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): FactAction {
        val storageProduct = extractStorageProduct(action, stepResults)
        val storagePallet = extractStoragePallet(action, stepResults)
        val placementPallet = extractPlacementPallet(action, stepResults)
        val placementBin = extractPlacementBin(action, stepResults)

        return FactAction(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            storageProduct = storageProduct,
            storagePallet = storagePallet,
            wmsAction = action.wmsAction,
            placementPallet = placementPallet,
            placementBin = placementBin,
            startedAt = stepResults["startedAt"] as? LocalDateTime ?: LocalDateTime.now().minusMinutes(1),
            completedAt = LocalDateTime.now(),
            plannedActionId = action.id
        )
    }

    private fun extractStorageProduct(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): TaskProduct? {
        for ((_, value) in stepResults) {
            if (value is TaskProduct) {
                return value
            } else if (value is Product) {
                return TaskProduct(product = value)
            }
        }

        return action.storageProduct
    }

    private fun extractStoragePallet(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): Pallet? {
        if (action.actionTemplate.storageObjectType == ActionObjectType.PALLET) {
            for ((key, value) in stepResults) {
                if (value is Pallet && key.contains("storage", ignoreCase = true)) {
                    return value
                }
            }

            for ((_, value) in stepResults) {
                if (value is Pallet) {
                    return value
                }
            }
        }

        return action.storagePallet
    }

    private fun extractPlacementPallet(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): Pallet? {
        if (action.actionTemplate.placementObjectType == ActionObjectType.PALLET) {
            for ((key, value) in stepResults) {
                if (value is Pallet && key.contains("placement", ignoreCase = true)) {
                    return value
                }
            }

            val storagePallet = extractStoragePallet(action, stepResults)
            for ((_, value) in stepResults) {
                if (value is Pallet && value != storagePallet) {
                    return value
                }
            }
        }

        return action.placementPallet
    }

    private fun extractPlacementBin(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): BinX? {
        if (action.actionTemplate.placementObjectType == ActionObjectType.BIN) {
            for ((_, value) in stepResults) {
                if (value is BinX) {
                    return value
                }
            }
        }

        return action.placementBin
    }
}