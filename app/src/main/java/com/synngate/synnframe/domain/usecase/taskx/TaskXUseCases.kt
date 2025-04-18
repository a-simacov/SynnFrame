package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.BinXRepository
import com.synngate.synnframe.domain.repository.PalletRepository
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDateTime

class TaskXUseCases(
    private val taskXRepository: TaskXRepository,
    private val taskTypeXRepository: TaskTypeXRepository,
    private val binXRepository: BinXRepository,
    private val palletRepository: PalletRepository
) : BaseUseCase {

    // Получение списка заданий
    fun getTasks(): Flow<List<TaskX>> = taskXRepository.getTasks()

    // Получение отфильтрованного списка заданий
    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskXStatus>? = null,
        typeFilter: List<String>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<TaskX>> {
        return taskXRepository.getFilteredTasks(
            nameFilter, statusFilter, typeFilter,
            dateFromFilter, dateToFilter, executorIdFilter
        )
    }

    // Получение задания по ID
    suspend fun getTaskById(id: String): TaskX? = taskXRepository.getTaskById(id)

    // Получение задания по штрихкоду
    suspend fun getTaskByBarcode(barcode: String): TaskX? = taskXRepository.getTaskByBarcode(barcode)

    // Количество заданий для текущего пользователя
    fun getTasksCountForCurrentUser(): Flow<Int> = taskXRepository.getTasksCountForCurrentUser()

    // Получение типа задания
    suspend fun getTaskType(taskTypeId: String): TaskTypeX? = taskTypeXRepository.getTaskTypeById(taskTypeId)

    // Начало выполнения задания
    suspend fun startTask(id: String, executorId: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Task was not found"))

            if (task.status != TaskXStatus.TO_DO) {
                Timber.w("It's impossible to start task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("Task has no status 'TO DO'"))
            }

            if (task.executorId != null && task.executorId != executorId) {
                Timber.w("Task '${task.name}' was assigned to another executor: ${task.executorId}")
                return Result.failure(IllegalStateException("Task was assigned to another executor"))
            }

            // Если исполнитель не назначен, проверяем доступность задания
            if (task.executorId == null) {
                try {
                    val availabilityResult = taskXRepository.checkTaskAvailability(id)
                    if (availabilityResult.isFailure || availabilityResult.getOrNull() != true) {
                        Timber.w("Task '${task.name}' is unavailable")
                        return Result.failure(IllegalStateException("Task is unavailable"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error on checking of the task availability")
                    // Если ошибка соединения, считаем что задание доступно
                    Timber.w("No connection to the server, task started without checking availability")
                }
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.IN_PROGRESS,
                startedAt = now,
                lastModifiedAt = now,
                executorId = executorId
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Task execution started: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Error on starting execution task")
            Result.failure(e)
        }
    }

    // Завершение выполнения задания
    suspend fun completeTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Task was not found"))

            val taskType = taskTypeXRepository.getTaskTypeById(task.taskTypeId)
                ?: return Result.failure(IllegalArgumentException("Task type was not found"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Impossible to complete the task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("The task is not in status 'In progress'"))
            }

            // Проверяем, есть ли строки факта или разрешено завершение без них
            if (task.factActions.isEmpty() && !taskType.allowCompletionWithoutFactActions) {
                Timber.w("Impossible to complete the task '${task.name}' without fact lines")
                return Result.failure(IllegalStateException("Impossible to complete the task without fact lines"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.COMPLETED,
                completedAt = now,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Finished task completion: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Error on completion task")
            Result.failure(e)
        }
    }

    // Приостановка выполнения задания
    suspend fun pauseTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Task was not found"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Impossible to pause the task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("The task is not in the status 'In progress'"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.PAUSED,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Task completion paused: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Error on pausing task")
            Result.failure(e)
        }
    }

    // Возобновление выполнения задания
    suspend fun resumeTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskXStatus.PAUSED) {
                Timber.w("Impossible to resume the task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("The task is not in the status 'Paused'"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.IN_PROGRESS,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Task completion resumed: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Error on resuming task")
            Result.failure(e)
        }
    }

    // Добавление строки факта
    suspend fun addFactAction(factAction: FactAction): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(factAction.taskId)
                ?: return Result.failure(IllegalArgumentException("Task was not found"))

            val taskType = taskTypeXRepository.getTaskTypeById(task.taskTypeId)
                ?: return Result.failure(IllegalArgumentException("Task type was not found"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Impossible to add fact action in the task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("The task is not in the status 'In progress'"))
            }

            // Проверка превышения планового количества, если это не разрешено
            if (!taskType.allowExceedPlanQuantity && factAction.storageProduct != null) {
                val product = factAction.storageProduct.product

                // Получаем плановые и фактические количества для данного продукта
                val plannedActions = task.plannedActions.filter {
                    it.storageProduct?.product?.id == product.id
                }

                val planQuantity = plannedActions
                    .mapNotNull { it.storageProduct?.quantity }
                    .sum()

                val factQuantity = task.factActions
                    .filter { it.storageProduct?.product?.id == product.id }
                    .mapNotNull { it.storageProduct?.quantity }
                    .sum() + (factAction.storageProduct.quantity)

                if (factQuantity > planQuantity) {
                    Timber.w("Plan qty exceeded for product ${product.name}")
                    return Result.failure(
                        IllegalStateException("Plan qty exceeded for product ${product.name}")
                    )
                }
            }

            // Добавляем действие факта
            taskXRepository.addFactAction(factAction)
            Timber.i("Fact action added in the task: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(factAction.taskId)!!)
        } catch (e: Exception) {
            Timber.e(e, "Error on adding fact action")
            Result.failure(e)
        }
    }

    // Верификация задания
    suspend fun verifyTask(id: String, barcode: String): Result<Boolean> {
        return taskXRepository.verifyTask(id, barcode)
    }

    // Создание паллеты
    suspend fun createPallet(): Result<Pallet> {
        return palletRepository.createPallet()
    }

    // Печать этикетки паллеты
    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return palletRepository.printPalletLabel(code)
    }

    // Закрытие паллеты
    suspend fun closePallet(code: String): Result<Boolean> {
        return palletRepository.closePallet(code)
    }

    // Получение ячейки по коду
    suspend fun getBinByCode(code: String): BinX? {
        return binXRepository.getBinByCode(code)
    }

    // Получение отфильтрованных ячеек
    fun getFilteredBins(codeFilter: String? = null, zoneFilter: String? = null): Flow<List<BinX>> {
        return binXRepository.getFilteredBins(codeFilter, zoneFilter)
    }

    // Проверка существования ячейки
    suspend fun binExists(code: String): Boolean {
        return binXRepository.binExists(code)
    }

    suspend fun getTaskTypeById(taskTypeId: String): TaskTypeX? {
        return taskTypeXRepository.getTaskTypeById(taskTypeId)
    }

    fun getTaskTypes(): Flow<List<TaskTypeX>> {
        return taskTypeXRepository.getTaskTypes()
    }
}