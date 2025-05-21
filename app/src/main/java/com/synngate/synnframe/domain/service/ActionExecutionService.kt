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
                    if (completeAction && plannedAction.getProgressType() == ProgressType.QUANTITY && plannedAction.storageProduct != null) {
                        val plannedQuantity = plannedAction.storageProduct.quantity

                        val completedQuantity = updatedFactActions
                            .filter { it.plannedActionId == plannedAction.id }
                            .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
                            .toFloat()

                        if (completedQuantity >= plannedQuantity) {
                            plannedAction.copy(
                                isCompleted = true,
                                manuallyCompleted = true,
                                manuallyCompletedAt = LocalDateTime.now()
                            )
                        } else {
                            plannedAction.copy(
                                isCompleted = false,
                                manuallyCompleted = false
                            )
                        }
                    } else if (completeAction) {
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
            plannedActionId = action.id,
            actionTemplateId = action.actionTemplate.id
        )
    }

    private fun extractStorageProduct(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): TaskProduct? {
        val hasStorageProductStep = action.actionTemplate.storageSteps.any {
            it.objectType == ActionObjectType.TASK_PRODUCT || it.objectType == ActionObjectType.CLASSIFIER_PRODUCT
        }

        if (!hasStorageProductStep) {
            return action.storageProduct
        }

        val lastTaskProduct = stepResults["lastTaskProduct"] as? TaskProduct
        if (lastTaskProduct != null && lastTaskProduct.quantity > 0f) {
            return lastTaskProduct
        }

        for ((key, value) in stepResults) {
            if (value is TaskProduct && value.quantity > 0f) {
                return value
            }
        }

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

        return action.storageProduct
    }

    private fun extractStoragePallet(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): Pallet? {
        val hasStoragePalletStep = action.actionTemplate.storageSteps.any {
            it.objectType == ActionObjectType.PALLET
        }

        if (!hasStoragePalletStep) {
            return action.storagePallet
        }

        for ((key, value) in stepResults) {
            if (value is Pallet && key.contains("storage", ignoreCase = true)) {
                return value
            }
        }

        for ((_, value) in stepResults) {
            if (value is Pallet) {
                val placementPallet = extractPlacementPallet(action, stepResults)
                if (placementPallet == null || value.code != placementPallet.code) {
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
        val hasPlacementPalletStep = action.actionTemplate.placementSteps.any {
            it.objectType == ActionObjectType.PALLET
        }

        if (!hasPlacementPalletStep) {
            return action.placementPallet
        }

        for ((key, value) in stepResults) {
            if (value is Pallet && key.contains("placement", ignoreCase = true)) {
                return value
            }
        }

        val storagePallet = extractStoragePallet(action, stepResults)
        for ((_, value) in stepResults) {
            if (value is Pallet && (storagePallet == null || value.code != storagePallet.code)) {
                return value
            }
        }

        return action.placementPallet
    }

    private fun extractPlacementBin(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): BinX? {
        val hasPlacementBinStep = action.actionTemplate.placementSteps.any {
            it.objectType == ActionObjectType.BIN
        }

        if (!hasPlacementBinStep) {
            return action.placementBin
        }

        for ((_, value) in stepResults) {
            if (value is BinX) {
                return value
            }
        }

        return action.placementBin
    }
}