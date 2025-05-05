package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
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

/**
 * Сервис для выполнения действий задания
 * Обновлен для поддержки множественных фактических действий
 */
class ActionExecutionService(
    private val validationService: ValidationService,
    private val taskContextManager: TaskContextManager,
    private val taskXRepository: TaskXRepository? = null
) {
    /**
     * Выполняет действие задания
     * @param taskId ID задания
     * @param actionId ID действия
     * @param stepResults Результаты шагов действия
     * @param completeAction Принудительно отметить действие как выполненное
     * @return Результат операции с обновленным заданием
     */
    suspend fun executeAction(
        taskId: String,
        actionId: String,
        stepResults: Map<String, Any>,
        completeAction: Boolean = false
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            // Получаем задание из контекста
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Проверяем, разрешены ли множественные фактические действия для этого типа задания
            val allowMultipleFactActions = task.taskType?.allowMultipleFactActions == true

            // Создаем фактическое действие на основе введенных данных
            val factAction = createFactAction(taskId, action, stepResults)

            // Получаем endpoint из контекста
            val endpoint = taskContextManager.currentEndpoint.value

            // Если есть endpoint и репозиторий, отправляем фактическое действие на сервер
            if (endpoint != null && taskXRepository != null) {
                Timber.d("Отправка фактического действия на сервер")
                val result = taskXRepository.addFactAction(factAction, endpoint)

                // Если запрос выполнен успешно, возвращаем результат
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    Timber.e(result.exceptionOrNull(), "Ошибка при отправке фактического действия на сервер")
                    return@withContext result
                }
            }

            // Если нет endpoint или репозитория, или запрос не выполнен,
            // выполняем локальное обновление
            Timber.d("Локальное обновление задания")
            val factActions = task.factActions.toMutableList()
            factActions.add(factAction)

            // Проверяем, нужно ли отмечать действие как выполненное
            val shouldCompleteAction = if (allowMultipleFactActions && action.progressType == ProgressType.QUANTITY) {
                // Для множественных фактических действий с учетом количества
                // проверяем достижение планового количества или принудительное завершение
                val plannedQuantity = action.storageProduct?.quantity ?: 0f
                val completedQuantity = factActions
                    .filter { it.plannedActionId == action.id }
                    .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
                    .toFloat()

                completeAction || completedQuantity >= plannedQuantity
            } else {
                // Для стандартных действий отмечаем как выполненное всегда
                true
            }

            // Обновляем запланированное действие, помечая его как выполненное при необходимости
            val updatedPlannedActions = task.plannedActions.map {
                if (it.id == actionId && shouldCompleteAction) {
                    // Если действие завершается вручную, сохраняем информацию об этом
                    if (completeAction && !it.isActionCompleted(factActions)) {
                        it.copy(
                            isCompleted = true,
                            manuallyCompleted = true,
                            manuallyCompletedAt = LocalDateTime.now(),
                        )
                    } else {
                        it.copy(isCompleted = true)
                    }
                } else {
                    it
                }
            }

            // Создаем обновленное задание
            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                factActions = factActions,
                lastModifiedAt = LocalDateTime.now()
            )

            // Обновляем данные в контексте
            taskContextManager.updateTask(updatedTask)

            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error executing action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    /**
     * Изменяет статус выполнения действия вручную
     * @param taskId ID задания
     * @param actionId ID действия
     * @param completed Новый статус выполнения
     * @return Результат операции с обновленным заданием
     */
    suspend fun setActionCompletionStatus(
        taskId: String,
        actionId: String,
        completed: Boolean
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            // Получаем задание из контекста
            val task = taskContextManager.lastStartedTaskX.value
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found in context: $taskId"))

            if (task.id != taskId) {
                return@withContext Result.failure(IllegalArgumentException("Task ID mismatch: expected $taskId, got ${task.id}"))
            }

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Проверяем, разрешены ли множественные фактические действия для этого типа задания
            val allowMultipleFactActions = task.taskType?.allowMultipleFactActions == true

            // Проверяем, можно ли изменить статус выполнения вручную
            if (!allowMultipleFactActions && action.progressType != ProgressType.QUANTITY) {
                return@withContext Result.failure(IllegalStateException("Manual completion status change is not allowed for this action"))
            }

            // Обновляем статус выполнения действия
            val updatedPlannedActions = task.plannedActions.map {
                if (it.id == actionId) {
                    if (completed) {
                        // Если отмечаем как выполненное
                        it.copy(
                            isCompleted = true,
                            manuallyCompleted = true,
                            manuallyCompletedAt = LocalDateTime.now(),
                        )
                    } else {
                        // Если снимаем отметку о выполнении
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

            // Создаем обновленное задание
            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                lastModifiedAt = LocalDateTime.now()
            )

            // Обновляем данные в контексте
            taskContextManager.updateTask(updatedTask)

            // Если есть репозиторий и endpoint, отправляем обновление на сервер
            val endpoint = taskContextManager.currentEndpoint.value
            if (endpoint != null && taskXRepository != null) {
                // Здесь можно добавить логику отправки обновления статуса на сервер
                // Например, вызвать специальный метод репозитория
            }

            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error changing action completion status: ${e.message}")
            Result.failure(e)
        }
    }

    // ... остальные методы класса остаются без изменений ...

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