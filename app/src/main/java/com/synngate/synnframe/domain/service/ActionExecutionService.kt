package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.ProgressType
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class ActionExecutionService(
    private val taskContextManager: TaskContextManager,
    private val taskXRepository: TaskXRepository? = null
) {

    suspend fun executeAction(
        taskId: String,
        actionId: String,
        stepResults: Map<String, Any>,
        completeAction: Boolean = false
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Логируем все TaskProduct в контексте для отладки
            stepResults.entries.forEach { (key, value) ->
                if (value is TaskProduct) {
                    Timber.d("TaskProduct in context: key=$key, product=${value.product.name}, quantity=${value.quantity}")
                }
            }

            val factAction = createFactAction(taskId, action, stepResults)

            val endpoint = taskContextManager.currentEndpoint.value

            val apiResult: Result<TaskX>?

            if (endpoint != null && taskXRepository != null) {
                apiResult = taskXRepository.addFactAction(factAction, endpoint)

                if (!apiResult.isSuccess) {
                    val errorMessage = apiResult.exceptionOrNull()?.message ?: "Ошибка при отправке данных на сервер"
                    Timber.e(apiResult.exceptionOrNull(), "Ошибка при отправке фактического действия на сервер: $errorMessage")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            }

            val updatedFactActions = task.factActions.toMutableList()
            updatedFactActions.add(factAction)

            val updatedPlannedActions = task.plannedActions.map { plannedAction ->
                if (plannedAction.id == actionId) {
                    if (completeAction) {
                        plannedAction.copy(
                            isCompleted = true,
                            manuallyCompleted = true,
                            manuallyCompletedAt = LocalDateTime.now()
                        )
                    } else {
                        val isCompleted = plannedAction.isActionCompleted(updatedFactActions)
                        plannedAction.copy(isCompleted = isCompleted)
                    }
                } else {
                    plannedAction
                }
            }

            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                factActions = updatedFactActions,
                lastModifiedAt = LocalDateTime.now()
            )

            taskContextManager.updateTask(updatedTask, skipStatusProcessing = true)

            return@withContext Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error executing action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    suspend fun setActionCompletionStatus(
        taskId: String,
        actionId: String,
        completed: Boolean
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            val taskType = taskContextManager.lastTaskTypeX.value
            val allowMultipleFactActions = taskType?.allowMultipleFactActions == true

            if (!allowMultipleFactActions || action.getProgressType() != ProgressType.QUANTITY) {
                return@withContext Result.failure(IllegalStateException("Manual completion status change is not allowed for this action"))
            }

            val completionTime = LocalDateTime.now()

            val updatedPlannedActions = task.plannedActions.map {
                if (it.id == actionId) {
                    if (completed) {
                        it.copy(
                            isCompleted = true,
                            manuallyCompleted = true,
                            manuallyCompletedAt = completionTime,
                        )
                    } else {
                        it.copy(
                            isCompleted = false,
                            manuallyCompleted = false,
                            manuallyCompletedAt = null,
                        )
                    }
                } else {
                    it
                }
            }

            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                lastModifiedAt = LocalDateTime.now()
            )

            taskContextManager.updateTask(updatedTask, skipStatusProcessing = true)

            val endpoint = taskContextManager.currentEndpoint.value
            if (endpoint != null && taskXRepository != null) {
                val requestDto = PlannedActionStatusRequestDto.fromPlannedAction(
                    plannedActionId = actionId,
                    manuallyCompleted = completed,
                    manuallyCompletedAt = if (completed) completionTime else null
                )

                try {
                    val apiResult = taskXRepository.setPlannedActionStatus(
                        taskId = taskId,
                        requestDto = requestDto,
                        endpoint = endpoint
                    )

                    if (!apiResult.isSuccess()) {
                        Timber.w("Failed to send action status update to server: ${(apiResult as ApiResult.Error).message}")
                        return@withContext Result.failure(Exception(apiResult.message))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error sending action status update to server")
                    return@withContext Result.failure(e)
                }
            }

            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error changing action completion status: ${e.message}")
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
        // 1. Сначала проверяем специальный ключ "lastTaskProduct", который должен содержать
        // самый актуальный TaskProduct с правильным количеством
        val lastTaskProduct = stepResults["lastTaskProduct"] as? TaskProduct
        if (lastTaskProduct != null && lastTaskProduct.quantity > 0f) {
            Timber.d("Using lastTaskProduct for action execution, quantity: ${lastTaskProduct.quantity}")
            return lastTaskProduct
        }

        // 2. Ищем любой TaskProduct с положительным количеством
        for ((key, value) in stepResults) {
            if (value is TaskProduct && value.quantity > 0f) {
                Timber.d("Found TaskProduct with quantity ${value.quantity} for key $key")
                return value
            }
        }

        // 3. Ищем Product и пытаемся создать из него TaskProduct
        for ((_, value) in stepResults) {
            if (value is Product) {
                var quantity = 0f
                var status = ProductStatus.STANDARD
                var expirationDate: LocalDateTime? = null

                for ((_, quantityValue) in stepResults) {
                    if (quantityValue is Float) {
                        quantity = quantityValue
                        break
                    } else if (quantityValue is Double) {
                        quantity = quantityValue.toFloat()
                        break
                    } else if (quantityValue is Int) {
                        quantity = quantityValue.toFloat()
                        break
                    } else if (quantityValue is String && quantityValue.toFloatOrNull() != null) {
                        quantity = quantityValue.toFloat()
                        break
                    }
                }

                if (quantity <= 0f) {
                    quantity = action.storageProduct?.quantity ?: 1f
                }

                for ((_, statusValue) in stepResults) {
                    if (statusValue is ProductStatus) {
                        status = statusValue
                        break
                    }
                }

                for ((_, dateValue) in stepResults) {
                    if (dateValue is LocalDateTime) {
                        expirationDate = dateValue
                        break
                    }
                }

                val finalExpirationDate = if (value.accountingModel == AccountingModel.BATCH && expirationDate != null) {
                    expirationDate
                } else {
                    // Если срок годности не требуется или не указан, используем значение по умолчанию
                    LocalDateTime.of(1970, 1, 1, 0, 0)
                }

                return TaskProduct(
                    product = value,
                    quantity = quantity,
                    status = status,
                    expirationDate = finalExpirationDate
                )
            }
        }

        // 4. Если ничего не нашли, возвращаем исходный StorageProduct из действия с логированием
        Timber.w("No suitable TaskProduct found, using original storage product from action")
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