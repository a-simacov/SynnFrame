package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
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
                ?: return Result.failure(IllegalArgumentException("Задание не найдено"))

            if (task.status != TaskXStatus.TO_DO) {
                Timber.w("Невозможно начать выполнение задания '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Задание не в статусе 'К выполнению'"))
            }

            if (task.executorId != null && task.executorId != executorId) {
                Timber.w("Задание '${task.name}' назначено другому исполнителю: ${task.executorId}")
                return Result.failure(IllegalStateException("Задание назначено другому исполнителю"))
            }

            // Если исполнитель не назначен, проверяем доступность задания
            if (task.executorId == null) {
                try {
                    val availabilityResult = taskXRepository.checkTaskAvailability(id)
                    if (availabilityResult.isFailure || availabilityResult.getOrNull() != true) {
                        Timber.w("Задание '${task.name}' недоступно")
                        return Result.failure(IllegalStateException("Задание недоступно"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при проверке доступности задания")
                    // Если ошибка соединения, считаем что задание доступно
                    Timber.w("Нет подключения к серверу, задание взято в работу без проверки")
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
            Timber.i("Начато выполнение задания: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при начале выполнения задания")
            Result.failure(e)
        }
    }

    // Завершение выполнения задания
    suspend fun completeTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Задание не найдено"))

            val taskType = taskTypeXRepository.getTaskTypeById(task.taskTypeId)
                ?: return Result.failure(IllegalArgumentException("Тип задания не найден"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Невозможно завершить задание '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
            }

            // Проверяем, есть ли строки факта или разрешено завершение без них
            if (task.factLines.isEmpty() && !taskType.allowCompletionWithoutFactLines) {
                Timber.w("Невозможно завершить задание '${task.name}' без строк факта")
                return Result.failure(IllegalStateException("Невозможно завершить задание без строк факта"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.COMPLETED,
                completedAt = now,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Завершено выполнение задания: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при завершении задания")
            Result.failure(e)
        }
    }

    // Приостановка выполнения задания
    suspend fun pauseTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Задание не найдено"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Невозможно приостановить задание '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.PAUSED,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Приостановлено выполнение задания: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при приостановке задания")
            Result.failure(e)
        }
    }

    // Возобновление выполнения задания
    suspend fun resumeTask(id: String): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(id)
                ?: return Result.failure(IllegalArgumentException("Задание не найдено"))

            if (task.status != TaskXStatus.PAUSED) {
                Timber.w("Невозможно возобновить задание '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Задание не в статусе 'Приостановлено'"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.copy(
                status = TaskXStatus.IN_PROGRESS,
                lastModifiedAt = now
            )

            taskXRepository.updateTask(updatedTask)
            Timber.i("Возобновлено выполнение задания: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при возобновлении задания")
            Result.failure(e)
        }
    }

    // Добавление строки факта
    suspend fun addFactLine(factLine: FactLineX): Result<TaskX> {
        return try {
            val task = taskXRepository.getTaskById(factLine.taskId)
                ?: return Result.failure(IllegalArgumentException("Задание не найдено"))

            val taskType = taskTypeXRepository.getTaskTypeById(task.taskTypeId)
                ?: return Result.failure(IllegalArgumentException("Тип задания не найден"))

            if (task.status != TaskXStatus.IN_PROGRESS) {
                Timber.w("Невозможно добавить строку факта к заданию '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
            }

            // Проверка превышения планового количества, если это не разрешено
            if (!taskType.allowExceedPlanQuantity && factLine.storageProduct != null) {
                val product = factLine.storageProduct.product
                val planQuantity = task.planLines
                    .filter { it.storageProduct?.product?.id == product.id }
                    .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }

                val factQuantity = task.factLines
                    .filter { it.storageProduct?.product?.id == product.id }
                    .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 } +
                        factLine.storageProduct.quantity

                if (factQuantity > planQuantity) {
                    Timber.w("Превышение планового количества для товара ${product.name}")
                    return Result.failure(
                        IllegalStateException("Превышение планового количества для товара ${product.name}")
                    )
                }
            }

            // Добавляем строку факта
            taskXRepository.addFactLine(factLine)
            Timber.i("Добавлена строка факта к заданию: ${task.name}")

            // Возвращаем обновленное задание
            Result.success(taskXRepository.getTaskById(factLine.taskId)!!)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении строки факта")
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
}