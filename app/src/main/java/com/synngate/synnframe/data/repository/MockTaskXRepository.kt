package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.PlanLineX
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class MockTaskXRepository(
    private val taskTypeXRepository: TaskTypeXRepository
) : TaskXRepository {

    companion object {
        private const val USER666ID = "8b858d00-f058-11ef-9bfd-000c2961fff3"
    }

    private val tasksFlow = MutableStateFlow<Map<String, TaskX>>(createInitialTasks())

    override fun getTasks(): Flow<List<TaskX>> {
        return tasksFlow.map { it.values.toList() }
    }

    override fun getFilteredTasks(
        nameFilter: String?,
        statusFilter: List<TaskXStatus>?,
        typeFilter: List<String>?,
        dateFromFilter: LocalDateTime?,
        dateToFilter: LocalDateTime?,
        executorIdFilter: String?
    ): Flow<List<TaskX>> {
        return tasksFlow.map { tasks ->
            tasks.values.filter { task ->
                (nameFilter == null || task.name.contains(nameFilter, ignoreCase = true)) &&
                        (statusFilter == null || task.status in statusFilter) &&
                        (typeFilter == null || task.taskTypeId in typeFilter) &&
                        (dateFromFilter == null || task.createdAt.isAfter(dateFromFilter)) &&
                        (dateToFilter == null || task.createdAt.isBefore(dateToFilter)) &&
                        (executorIdFilter == null || task.executorId == executorIdFilter)
            }
        }
    }

    override suspend fun getTaskById(id: String): TaskX? {
        return tasksFlow.value[id]
    }

    override suspend fun getTaskByBarcode(barcode: String): TaskX? {
        return tasksFlow.value.values.find { it.barcode == barcode }
    }

    override fun getTasksCountForCurrentUser(): Flow<Int> {
        // Здесь можно использовать какой-то текущий ID пользователя,
        // но для упрощения просто вернем количество заданий в статусе TO_DO
        return tasksFlow.map { tasks ->
            tasks.values.count { it.status == TaskXStatus.TO_DO }
        }
    }

    override suspend fun addTask(task: TaskX) {
        val updatedTasks = tasksFlow.value.toMutableMap()
        updatedTasks[task.id] = task
        tasksFlow.value = updatedTasks
    }

    override suspend fun updateTask(task: TaskX) {
        addTask(task) // Same implementation for mock
    }

    override suspend fun deleteTask(id: String) {
        val updatedTasks = tasksFlow.value.toMutableMap()
        updatedTasks.remove(id)
        tasksFlow.value = updatedTasks
    }

    override suspend fun setTaskStatus(id: String, status: TaskXStatus) {
        val task = tasksFlow.value[id] ?: return
        val updatedTask = task.copy(
            status = status,
            lastModifiedAt = LocalDateTime.now()
        )
        updateTask(updatedTask)
    }

    override suspend fun assignExecutor(id: String, executorId: String) {
        val task = tasksFlow.value[id] ?: return
        val updatedTask = task.copy(
            executorId = executorId,
            lastModifiedAt = LocalDateTime.now()
        )
        updateTask(updatedTask)
    }

    override suspend fun addFactLine(factLine: FactLineX) {
        val task = tasksFlow.value[factLine.taskId] ?: return

        val updatedFactLines = task.factLines.toMutableList()
        updatedFactLines.add(factLine)

        val updatedTask = task.copy(
            factLines = updatedFactLines,
            lastModifiedAt = LocalDateTime.now()
        )

        updateTask(updatedTask)
    }

    override suspend fun setStartTime(id: String, startTime: LocalDateTime) {
        val task = tasksFlow.value[id] ?: return
        val updatedTask = task.copy(
            startedAt = startTime,
            lastModifiedAt = LocalDateTime.now()
        )
        updateTask(updatedTask)
    }

    override suspend fun setCompletionTime(id: String, completionTime: LocalDateTime) {
        val task = tasksFlow.value[id] ?: return
        val updatedTask = task.copy(
            completedAt = completionTime,
            lastModifiedAt = LocalDateTime.now()
        )
        updateTask(updatedTask)
    }

    override suspend fun checkTaskAvailability(id: String): Result<Boolean> {
        // Имитация обращения к серверу
        delay(800)

        val task = tasksFlow.value[id]
        if (task == null) {
            Timber.w("Попытка проверки доступности несуществующего задания: $id")
            return Result.failure(NoSuchElementException("Задание не найдено: $id"))
        }

        // Для примера, будем считать, что задание доступно, если оно
        // еще не назначено исполнителю или не начато
        val isAvailable = task.executorId == null && task.status == TaskXStatus.TO_DO

        return Result.success(isAvailable)
    }

    override suspend fun verifyTask(id: String, barcode: String): Result<Boolean> {
        // Имитация обращения к серверу
        delay(500)

        val task = tasksFlow.value[id]
        if (task == null) {
            Timber.w("Попытка верификации несуществующего задания: $id")
            return Result.failure(NoSuchElementException("Задание не найдено: $id"))
        }

        // Проверяем, совпадает ли штрихкод
        val isVerified = task.barcode == barcode

        if (isVerified) {
            // Обновляем задание, если верификация успешна
            val updatedTask = task.copy(
                isVerified = true,
                lastModifiedAt = LocalDateTime.now()
            )
            updateTask(updatedTask)

            Timber.i("Задание $id успешно верифицировано")
        } else {
            Timber.w("Неверный штрихкод при верификации задания $id: $barcode")
        }

        return Result.success(isVerified)
    }

    // Обновим метод createInitialTasks в MockTaskXRepository
    private fun createInitialTasks(): Map<String, TaskX> {
        val tasks = mutableMapOf<String, TaskX>()

        // Добавим тестовое задание по приемке из примера
        val receiptTask = createReceiptTask()
        tasks[receiptTask.id] = receiptTask

        // Добавим еще несколько тестовых заданий разных типов
        val pickingTask = createPickingTask()
        tasks[pickingTask.id] = pickingTask

        val movementTask = createMovementTask()
        tasks[movementTask.id] = movementTask

        return tasks
    }

    // Создание задания "Отбор заказа"
    private fun createPickingTask(): TaskX {
        // Создаем продукты для строк плана
        val productTV = Product(
            id = "p3",
            name = "Телевизор 55\"",
            accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
            articleNumber = "TV-55001",
            mainUnitId = "u3",
            units = emptyList()
        )

        val productPhone = Product(
            id = "p4",
            name = "Смартфон",
            accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
            articleNumber = "PH-12345",
            mainUnitId = "u4",
            units = emptyList()
        )

        // Создаем товары задания для строк плана
        val taskProductTV = TaskProduct(
            product = productTV,
            quantity = 2f
        )

        val taskProductPhone = TaskProduct(
            product = productPhone,
            quantity = 5f
        )

        // Создаем строки плана
        val planLine1 = PlanLineX(
            id = UUID.randomUUID().toString(),
            taskId = "task2",
            executionOrder = 1,
            storageProduct = taskProductTV,
            wmsAction = WmsAction.TAKE_FROM,
            placementBin = BinX(
                code = "C00311",
                zone = "Отбор",
                line = "C",
                rack = "03",
                tier = "1",
                position = "1"
            )
        )

        val planLine2 = PlanLineX(
            id = UUID.randomUUID().toString(),
            taskId = "task2",
            executionOrder = 2,
            storageProduct = taskProductPhone,
            wmsAction = WmsAction.TAKE_FROM,
            placementBin = BinX(
                code = "C00312",
                zone = "Отбор",
                line = "C",
                rack = "03",
                tier = "1",
                position = "2"
            )
        )

        // Создаем задание
        return TaskX(
            id = "task2",
            barcode = "03165467988",
            name = "Отбор заказа №12345",
            taskTypeId = "7891011121314", // ID типа задания "Отбор заказа"
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusHours(3),
            executorId = USER666ID,
            planLines = listOf(planLine1, planLine2)
        )
    }

    // Создание задания "Перемещение"
    private fun createMovementTask(): TaskX {
        // Создаем продукт для строк плана
        val productLaptop = Product(
            id = "p5",
            name = "Ноутбук",
            accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
            articleNumber = "LT-9876",
            mainUnitId = "u5",
            units = emptyList()
        )

        // Создаем товар задания для строк плана
        val taskProductLaptop = TaskProduct(
            product = productLaptop,
            quantity = 10f
        )

        // Создаем строку плана
        val planLine = PlanLineX(
            id = UUID.randomUUID().toString(),
            taskId = "task3",
            executionOrder = 0,
            storageProduct = taskProductLaptop,
            wmsAction = WmsAction.TAKE_FROM,
            placementBin = BinX(
                code = "B00211",
                zone = "Хранение",
                line = "B",
                rack = "02",
                tier = "1",
                position = "1"
            )
        )

        // Создаем задание
        return TaskX(
            id = "task3",
            barcode = "03165467989",
            name = "Перемещение товара",
            taskTypeId = "8910111213141", // ID типа задания "Перемещение"
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusHours(1),
            executorId = USER666ID,
            planLines = listOf(planLine)
        )
    }

    // Создание задания "Приемка по монопалетам" из примера
    private fun createReceiptTask(): TaskX {
        // Создаем продукты для строк плана
        val productHeadphones = Product(
            id = "p1",
            name = "Наушники вкладыши",
            accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
            articleNumber = "H-12345",
            mainUnitId = "u1",
            units = emptyList()
        )

        val productMilk = Product(
            id = "p2",
            name = "Молоко",
            accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
            articleNumber = "M-67890",
            mainUnitId = "u2",
            units = emptyList()
        )

        // Создаем товары задания для строк плана
        val taskProductHeadphones = TaskProduct(
            product = productHeadphones,
            quantity = 26f
        )

        val taskProductMilk = TaskProduct(
            product = productMilk,
            quantity = 18f
        )

        // Создаем строки плана
        val planLine1 = PlanLineX(
            id = UUID.randomUUID().toString(),
            taskId = "task1",
            executionOrder = 0,
            storageProduct = taskProductHeadphones,
            wmsAction = WmsAction.RECEIPT,
            placementBin = null // В примере указано "Ячейка с условием (из зоны приемки)"
        )

        val planLine2 = PlanLineX(
            id = UUID.randomUUID().toString(),
            taskId = "task1",
            executionOrder = 0,
            storageProduct = taskProductMilk,
            wmsAction = WmsAction.RECEIPT,
            placementBin = null // В примере указано "Ячейка с условием (из зоны приемки)"
        )

        // Создаем задание
        return TaskX(
            id = "task1",
            barcode = "03165467987",
            name = "Принять задание по монопалетам от Клиента 1",
            taskTypeId = "6546513215648", // ID типа задания из примера
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusDays(1),
            executorId = USER666ID,
            planLines = listOf(planLine1, planLine2)
        )
    }
}