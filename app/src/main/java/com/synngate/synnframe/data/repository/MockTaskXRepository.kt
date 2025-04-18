package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRuleItem
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import com.synngate.synnframe.domain.repository.TaskXRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime

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
        addTask(task)
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

    // Новые методы для работы с действиями

    override suspend fun addFactAction(factAction: FactAction) {
        val task = tasksFlow.value[factAction.taskId] ?: return

        val updatedFactActions = task.factActions.toMutableList()
        updatedFactActions.add(factAction)

        val updatedTask = task.copy(
            factActions = updatedFactActions,
            lastModifiedAt = LocalDateTime.now()
        )

        updateTask(updatedTask)
    }

    override suspend fun getPlannedActionById(taskId: String, actionId: String): PlannedAction? {
        val task = tasksFlow.value[taskId] ?: return null
        return task.plannedActions.find { it.id == actionId }
    }

    override suspend fun updatePlannedAction(taskId: String, action: PlannedAction) {
        val task = tasksFlow.value[taskId] ?: return

        val updatedPlannedActions = task.plannedActions.toMutableList()
        val index = updatedPlannedActions.indexOfFirst { it.id == action.id }

        if (index != -1) {
            updatedPlannedActions[index] = action

            val updatedTask = task.copy(
                plannedActions = updatedPlannedActions,
                lastModifiedAt = LocalDateTime.now()
            )

            updateTask(updatedTask)
        }
    }

    override suspend fun markPlannedActionCompleted(taskId: String, actionId: String, isCompleted: Boolean) {
        val task = tasksFlow.value[taskId] ?: return
        val action = task.plannedActions.find { it.id == actionId } ?: return

        val updatedAction = action.copy(isCompleted = isCompleted)
        updatePlannedAction(taskId, updatedAction)
    }

    override suspend fun markPlannedActionSkipped(taskId: String, actionId: String, isSkipped: Boolean) {
        val task = tasksFlow.value[taskId] ?: return
        val action = task.plannedActions.find { it.id == actionId } ?: return

        val updatedAction = action.copy(isSkipped = isSkipped)
        updatePlannedAction(taskId, updatedAction)
    }

    override suspend fun getNextPlannedAction(taskId: String): PlannedAction? {
        val task = tasksFlow.value[taskId] ?: return null
        return task.plannedActions.firstOrNull { !it.isCompleted && !it.isSkipped }
    }

    // Метод создания тестовых заданий
    private fun createInitialTasks(): Map<String, TaskX> {
        val tasks = mutableMapOf<String, TaskX>()

        // Создаем задание "Перемещение паллеты с подтверждением товара" из примера
        val movePalletTask = createMovePalletTask()
        tasks[movePalletTask.id] = movePalletTask

        // Добавляем ещё тестовые задания из старого кода
        val receiptTask = createReceiptTask()
        tasks[receiptTask.id] = receiptTask

        val pickingTask = createPickingTask()
        tasks[pickingTask.id] = pickingTask

        val movementTask = createMovementTask()
        tasks[movementTask.id] = movementTask

        return tasks
    }

    // Создание задания "Перемещение паллеты с подтверждением товара" из примера
    private fun createMovePalletTask(): TaskX {
        // Подготовим правила валидации
        val fromPlanValidationRule = ValidationRule(
            name = "Из плана",
            rules = listOf(
                ValidationRuleItem(
                    type = ValidationType.FROM_PLAN,
                    errorMessage = "Выберите объект из плана"
                ),
                ValidationRuleItem(
                    type = ValidationType.NOT_EMPTY,
                    errorMessage = "Объект не должен быть пустым"
                )
            )
        )

        val notEmptyValidationRule = ValidationRule(
            name = "Заполнена",
            rules = listOf(
                ValidationRuleItem(
                    type = ValidationType.NOT_EMPTY,
                    errorMessage = "Объект не должен быть пустым"
                )
            )
        )

        // Шаблоны действий
        val takePalletTemplate = ActionTemplate(
            id = "template_take_pallet",
            name = "Взять определенную паллету из определенной ячейки",
            wmsAction = WmsAction.TAKE_FROM,
            storageObjectType = ActionObjectType.PALLET,
            placementObjectType = ActionObjectType.BIN,
            storageSteps = listOf(
                ActionStep(
                    id = "step_select_planned_pallet",
                    order = 1,
                    name = "Выберите паллету из запланированного действия",
                    promptText = "Выберите паллету из запланированного действия",
                    objectType = ActionObjectType.PALLET,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = listOf(
                ActionStep(
                    id = "step_select_planned_bin",
                    order = 2,
                    name = "Выберите ячейку из запланированного действия",
                    promptText = "Выберите ячейку из запланированного действия",
                    objectType = ActionObjectType.BIN,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            )
        )

        val confirmProductTemplate = ActionTemplate(
            id = "template_confirm_product",
            name = "Подтвердить наличие товара",
            wmsAction = WmsAction.ASSERT,
            storageObjectType = ActionObjectType.CLASSIFIER_PRODUCT,
            placementObjectType = null,
            storageSteps = listOf(
                ActionStep(
                    id = "step_confirm_product",
                    order = 1,
                    name = "Подтвердите наличие товара",
                    promptText = "Подтвердите наличие товара",
                    objectType = ActionObjectType.CLASSIFIER_PRODUCT,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = emptyList()
        )

        val putPalletTemplate = ActionTemplate(
            id = "template_put_pallet",
            name = "Положить определенную паллету в ячейку хранения",
            wmsAction = WmsAction.PUT_INTO,
            storageObjectType = ActionObjectType.PALLET,
            placementObjectType = ActionObjectType.BIN,
            storageSteps = listOf(
                ActionStep(
                    id = "step_select_planned_pallet",
                    order = 1,
                    name = "Выберите паллету из запланированного действия",
                    promptText = "Выберите паллету из запланированного действия",
                    objectType = ActionObjectType.PALLET,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = listOf(
                ActionStep(
                    id = "step_select_bin",
                    order = 2,
                    name = "Выберите ячейку",
                    promptText = "Выберите ячейку",
                    objectType = ActionObjectType.BIN,
                    validationRules = notEmptyValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            )
        )

        // Запланированные действия
        val plannedActions = listOf(
            PlannedAction(
                id = "action1",
                order = 1,
                actionTemplate = takePalletTemplate,
                storagePallet = Pallet(code = "IN00000000003", isClosed = false),
                wmsAction = WmsAction.TAKE_FROM,
                placementBin = BinX(
                    code = "R30111",
                    zone = "Хранение",
                    line = "R",
                    rack = "3",
                    tier = "01",
                    position = "11"
                )
            ),
            PlannedAction(
                id = "action2",
                order = 2,
                actionTemplate = confirmProductTemplate,
                storageProduct = TaskProduct(
                    product = Product(
                        id = "p_gel",
                        name = "Гель для стирки Active Universal 4,5 L",
                        accountingModel = AccountingModel.QTY,
                        articleNumber = "G-12345",
                        mainUnitId = "u_gel",
                        units = emptyList()
                    )
                ),
                wmsAction = WmsAction.ASSERT
            ),
            PlannedAction(
                id = "action3",
                order = 3,
                actionTemplate = putPalletTemplate,
                storagePallet = Pallet(code = "IN00000000003", isClosed = false),
                wmsAction = WmsAction.PUT_INTO
            )
        )

        // Создание задания
        return TaskX(
            id = "task_move_pallet",
            barcode = "03165467987",
            name = "Принять задание по монопалетам от Клиента 1",
            taskTypeId = "task_type_move_pallet",
            executorId = "admin",
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusDays(1),
            plannedActions = plannedActions,
            factActions = emptyList(),
            finalActions = emptyList(),
            allowCompletionWithoutFactActions = false
        )
    }

    // Сохраняем методы из старой реализации для других тестовых заданий
    private fun createReceiptTask(): TaskX {
        // ... код старого метода для совместимости ...
        // Создаем продукты для строк плана
        val productHeadphones = Product(
            id = "p1",
            name = "Наушники вкладыши",
            accountingModel = AccountingModel.QTY,
            articleNumber = "H-12345",
            mainUnitId = "u1",
            units = emptyList()
        )

        val productMilk = Product(
            id = "p2",
            name = "Молоко",
            accountingModel = AccountingModel.QTY,
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

        // Создаем задание (с новыми полями)
        return TaskX(
            id = "task1",
            barcode = "03165467987",
            name = "Принять задание по монопалетам от Клиента 1",
            taskTypeId = "6546513215648", // ID типа задания из примера
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusDays(1),
            executorId = USER666ID,
            plannedActions = emptyList(), // Пустые списки для совместимости
            factActions = emptyList(),
            finalActions = emptyList()
        )
    }

    private fun createPickingTask(): TaskX {
        // ... код старого метода для совместимости ...
        // Создаем продукты для строк плана
        val productTV = Product(
            id = "p3",
            name = "Телевизор 55\"",
            accountingModel = AccountingModel.QTY,
            articleNumber = "TV-55001",
            mainUnitId = "u3",
            units = emptyList()
        )

        val productPhone = Product(
            id = "p4",
            name = "Смартфон",
            accountingModel = AccountingModel.QTY,
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

        // Создаем задание (с новыми полями)
        return TaskX(
            id = "task2",
            barcode = "03165467988",
            name = "Отбор заказа №12345",
            taskTypeId = "7891011121314", // ID типа задания "Отбор заказа"
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusHours(3),
            executorId = USER666ID,
            plannedActions = emptyList(), // Пустые списки для совместимости
            factActions = emptyList(),
            finalActions = emptyList()
        )
    }

    private fun createMovementTask(): TaskX {
        // ... код старого метода для совместимости ...
        // Создаем продукт для строк плана
        val productLaptop = Product(
            id = "p5",
            name = "Ноутбук",
            accountingModel = AccountingModel.QTY,
            articleNumber = "LT-9876",
            mainUnitId = "u5",
            units = emptyList()
        )

        // Создаем задание (с новыми полями)
        return TaskX(
            id = "task3",
            barcode = "03165467989",
            name = "Перемещение товара",
            taskTypeId = "8910111213141", // ID типа задания "Перемещение"
            status = TaskXStatus.TO_DO,
            createdAt = LocalDateTime.now().minusHours(1),
            executorId = USER666ID,
            plannedActions = emptyList(), // Пустые списки для совместимости
            factActions = emptyList(),
            finalActions = emptyList()
        )
    }
}