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
    private val validationService: ValidationService
) {
    /**
     * Выполнить запланированное действие и создать фактическое действие
     * @param taskId Идентификатор задания
     * @param actionId Идентификатор запланированного действия
     * @param stepResults Результаты выполнения шагов
     * @return Результат с обновленным заданием или ошибкой
     */
    suspend fun executeAction(
        taskId: String,
        actionId: String,
        stepResults: Map<String, Any>
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Executing action $actionId for task $taskId")

            // Получение задания и запланированного действия
            val task = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found: $taskId"))

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Создание фактического действия на основе результатов шагов
            val factAction = createFactAction(taskId, action, stepResults)

            // Добавление фактического действия
            taskXRepository.addFactAction(factAction)

            // Отметка запланированного действия как выполненного
            taskXRepository.markPlannedActionCompleted(taskId, actionId, true)

            // Получение обновленного задания
            val updatedTask = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalStateException("Task not found after action execution"))

            Timber.i("Action $actionId successfully executed for task $taskId")
            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error executing action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    /**
     * Пропустить запланированное действие
     * @param taskId Идентификатор задания
     * @param actionId Идентификатор запланированного действия
     * @return Результат с обновленным заданием или ошибкой
     */
    suspend fun skipAction(
        taskId: String,
        actionId: String
    ): Result<TaskX> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Skipping action $actionId for task $taskId")

            // Проверка существования задания и действия
            val task = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalArgumentException("Task not found: $taskId"))

            val action = task.plannedActions.find { it.id == actionId }
                ?: return@withContext Result.failure(IllegalArgumentException("Planned action not found: $actionId"))

            // Отметка запланированного действия как пропущенного
            taskXRepository.markPlannedActionSkipped(taskId, actionId, true)

            // Получение обновленного задания
            val updatedTask = taskXRepository.getTaskById(taskId)
                ?: return@withContext Result.failure(IllegalStateException("Task not found after skipping action"))

            Timber.i("Action $actionId successfully skipped for task $taskId")
            Result.success(updatedTask)
        } catch (e: Exception) {
            Timber.e(e, "Error skipping action $actionId for task $taskId")
            Result.failure(e)
        }
    }

    /**
     * Создает фактическое действие на основе результатов шагов
     */
    private fun createFactAction(
        taskId: String,
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): FactAction {
        // Получаем данные из результатов шагов или из запланированного действия, если они отсутствуют
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

    /**
     * Извлекает товар хранения из результатов шагов или запланированного действия
     */
    private fun extractStorageProduct(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): TaskProduct? {
        // Проверяем, есть ли товар в результатах шагов
        for (entry in stepResults) {
            if (entry.value is TaskProduct) {
                return entry.value
            } else if (entry.value is Product) {
                // Если в результатах есть Product, преобразуем его в TaskProduct
                return TaskProduct(product = entry.value)
            }
        }

        // Если в результатах нет, возвращаем из запланированного действия
        return action.storageProduct
    }

    /**
     * Извлекает паллету хранения из результатов шагов или запланированного действия
     */
    private fun extractStoragePallet(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): Pallet? {
        // Проверяем, указан ли тип объекта хранения как паллета
        if (action.actionTemplate.storageObjectType == ActionObjectType.PALLET) {
            // Ищем в результатах шагов с пометкой "storage"
            for (entry in stepResults) {
                if (entry.value is Pallet && entry.key.contains("storage", ignoreCase = true)) {
                    return entry.value
                }
            }

            // Ищем любую паллету в результатах
            for (entry in stepResults) {
                if (entry.value is Pallet) {
                    return entry.value
                }
            }
        }

        // Если в результатах нет, возвращаем из запланированного действия
        return action.storagePallet
    }

    /**
     * Извлекает паллету размещения из результатов шагов или запланированного действия
     */
    private fun extractPlacementPallet(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): Pallet? {
        // Проверяем, указан ли тип объекта размещения как паллета
        if (action.actionTemplate.placementObjectType == ActionObjectType.PALLET) {
            // Ищем в результатах шагов с пометкой "placement"
            for (entry in stepResults) {
                if (entry.value is Pallet && entry.key.contains("placement", ignoreCase = true)) {
                    return entry.value
                }
            }

            // Если нет явной пометки "placement", но есть паллета, которая отличается от паллеты хранения
            val storagePallet = extractStoragePallet(action, stepResults)
            for (entry in stepResults) {
                if (entry.value is Pallet && entry.value != storagePallet) {
                    return entry.value
                }
            }
        }

        // Если в результатах нет, возвращаем из запланированного действия
        return action.placementPallet
    }

    /**
     * Извлекает ячейку размещения из результатов шагов или запланированного действия
     */
    private fun extractPlacementBin(
        action: PlannedAction,
        stepResults: Map<String, Any>
    ): BinX? {
        // Проверяем, указан ли тип объекта размещения как ячейка
        if (action.actionTemplate.placementObjectType == ActionObjectType.BIN) {
            // Ищем в результатах шагов
            for (entry in stepResults) {
                if (entry.value is BinX) {
                    return entry.value
                }
            }
        }

        // Если в результатах нет, возвращаем из запланированного действия
        return action.placementBin
    }
}