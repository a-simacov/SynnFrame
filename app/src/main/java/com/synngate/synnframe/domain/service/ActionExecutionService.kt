package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Сервис для выполнения действий задания
 */
class ActionExecutionService(
    private val taskXRepository: TaskXRepository,
    private val validationService: ValidationService,
    private val taskContextManager: TaskContextManager // Добавлен TaskContextManager
) {
    suspend fun executeAction(
        taskId: String,
        actionId: String,
        stepResults: Map<String, Any>
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            val contextTask = taskContextManager.lastStartedTaskX.value

            val task = if (contextTask != null && contextTask.id == taskId) {
                contextTask
            } else {
                taskXRepository.getTaskById(taskId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Task not found: $taskId"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            val factAction = createFactAction(taskId, action, stepResults)

            taskXRepository.addFactAction(factAction)

            taskXRepository.markPlannedActionCompleted(taskId, actionId, true)

            val updatedTask = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalStateException("Task not found after action execution"))

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
            val contextTask = taskContextManager.lastStartedTaskX.value

            val task = if (contextTask != null && contextTask.id == taskId) {
                contextTask
            } else {
                taskXRepository.getTaskById(taskId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Task not found: $taskId"))
            }

            taskXRepository.markPlannedActionSkipped(taskId, actionId, true)

            val updatedTask = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalStateException("Task not found after skipping action"))

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