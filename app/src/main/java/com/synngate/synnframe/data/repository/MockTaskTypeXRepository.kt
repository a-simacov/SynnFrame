package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.domain.entity.taskx.ObjectSelectionCondition
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.WmsOperation
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MockTaskTypeXRepository : TaskTypeXRepository {

    private val taskTypesFlow = MutableStateFlow<Map<String, TaskTypeX>>(createInitialTaskTypes())

    override fun getTaskTypes(): Flow<List<TaskTypeX>> {
        return taskTypesFlow.map { it.values.toList() }
    }

    override suspend fun getTaskTypeById(id: String): TaskTypeX? {
        return taskTypesFlow.value[id]
    }

    override suspend fun addTaskType(taskType: TaskTypeX) {
        val updatedMap = taskTypesFlow.value.toMutableMap()
        updatedMap[taskType.id] = taskType
        taskTypesFlow.value = updatedMap
    }

    override suspend fun updateTaskType(taskType: TaskTypeX) {
        addTaskType(taskType) // Same implementation for mock
    }

    override suspend fun deleteTaskType(id: String) {
        val updatedMap = taskTypesFlow.value.toMutableMap()
        updatedMap.remove(id)
        taskTypesFlow.value = updatedMap
    }

    // Создание типа задания "Приемка по монопалетам"
    private fun createReceiptTaskType(): TaskTypeX {
        // Создадим группу действий "Выбрать товар приемки"
        val selectProductGroup = FactLineActionGroup(
            id = "564321657",
            name = "Выбрать товар приемки",
            order = 1,
            targetFieldType = TaskXLineFieldType.STORAGE_PRODUCT,
            wmsAction = WmsAction.RECEIPT,
            resultType = "TaskProduct",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать товар из классификатора",
                    actionType = FactLineXActionType.SELECT_PRODUCT,
                    order = 1,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Выберите товар из плана задания"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Ввести срок годности",
                    actionType = FactLineXActionType.ENTER_EXPIRATION_DATE,
                    order = 2,
                    promptText = "Введите срок годности (если требуется)"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать статус товара",
                    actionType = FactLineXActionType.SELECT_PRODUCT_STATUS,
                    order = 3,
                    promptText = "Выберите статус товара"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Ввести количество",
                    actionType = FactLineXActionType.ENTER_QUANTITY,
                    order = 4,
                    promptText = "Введите количество товара"
                )
            )
        )

        // Создадим группу действий "Добавить паллету приемки"
        val addPalletGroup = FactLineActionGroup(
            id = "564321658",
            name = "Добавить паллету приемки",
            order = 2,
            targetFieldType = TaskXLineFieldType.PLACEMENT_PALLET,
            wmsAction = WmsAction.RECEIPT,
            resultType = "Pallet",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Создать паллету",
                    actionType = FactLineXActionType.CREATE_PALLET,
                    order = 1,
                    promptText = "Создание новой паллеты"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Печать этикетки",
                    actionType = FactLineXActionType.PRINT_LABEL,
                    order = 2,
                    promptText = "Печать этикетки паллеты"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Закрыть паллету",
                    actionType = FactLineXActionType.CLOSE_PALLET,
                    order = 3,
                    promptText = "Закрытие паллеты"
                )
            )
        )

        // Создадим группу действий "Выбрать ячейку приемки"
        val selectBinGroup = FactLineActionGroup(
            id = "564321659",
            name = "Выбрать ячейку приемки",
            order = 3,
            targetFieldType = TaskXLineFieldType.PLACEMENT_BIN,
            wmsAction = WmsAction.PUT_INTO,
            resultType = "BinX",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать ячейку",
                    actionType = FactLineXActionType.SELECT_BIN,
                    order = 1,
                    promptText = "Выберите ячейку из зоны приемки",
                    additionalParams = mapOf("zone" to "Приемка")
                )
            )
        )

        return TaskTypeX(
            id = "6546513215648",
            name = "Приемка по монопалетам",
            wmsOperation = WmsOperation.RECEIPT,
            canBeCreatedInApp = false,
            allowCompletionWithoutFactLines = false,
            allowExceedPlanQuantity = false,
            availableActions = listOf(
                AvailableTaskAction.PAUSE,
                AvailableTaskAction.RESUME,
                AvailableTaskAction.SHOW_PLAN_LINES,
                AvailableTaskAction.SHOW_FACT_LINES,
                AvailableTaskAction.COMPARE_LINES
            ),
            factLineActionGroups = listOf(
                selectProductGroup,
                addPalletGroup,
                selectBinGroup
            ),
            finalActions = emptyList()
        )
    }

    // Создание типа задания "Отбор заказа"
    private fun createPickingTaskType(): TaskTypeX {
        // Создаем группу действий "Взять товар из ячейки"
        val takeProdGroup = FactLineActionGroup(
            id = UUID.randomUUID().toString(),
            name = "Взять товар из ячейки",
            order = 1,
            targetFieldType = TaskXLineFieldType.STORAGE_PRODUCT,
            wmsAction = WmsAction.TAKE_FROM,
            resultType = "TaskProduct",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать товар",
                    actionType = FactLineXActionType.SELECT_PRODUCT,
                    order = 1,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Выберите товар"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Введите количество",
                    actionType = FactLineXActionType.ENTER_QUANTITY,
                    order = 2,
                    promptText = "Введите количество товара"
                )
            )
        )

        // Создаем группу действий "Выбрать ячейку размещения"
        val selectPlaceBinGroup = FactLineActionGroup(
            id = UUID.randomUUID().toString(),
            name = "Выбрать ячейку размещения",
            order = 2,
            targetFieldType = TaskXLineFieldType.PLACEMENT_BIN,
            wmsAction = WmsAction.TAKE_FROM,
            resultType = "BinX",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать ячейку",
                    actionType = FactLineXActionType.SELECT_BIN,
                    order = 1,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Отсканируйте или выберите ячейку"
                ),
            )
        )

        return TaskTypeX(
            id = "7891011121314",
            name = "Отбор заказа",
            wmsOperation = WmsOperation.PICKING,
            canBeCreatedInApp = false,
            allowCompletionWithoutFactLines = false,
            allowExceedPlanQuantity = false,
            availableActions = listOf(
                AvailableTaskAction.PAUSE,
                AvailableTaskAction.RESUME,
                AvailableTaskAction.SHOW_PLAN_LINES,
                AvailableTaskAction.SHOW_FACT_LINES,
                AvailableTaskAction.COMPARE_LINES,
                AvailableTaskAction.VERIFY_TASK
            ),
            factLineActionGroups = listOf(selectPlaceBinGroup, takeProdGroup),
            finalActions = emptyList()
        )
    }

    // Создание типа задания "Перемещение"
    private fun createMovementTaskType(): TaskTypeX {
        // Создаем группу действий "Взять товар"
        val takeFromGroup = FactLineActionGroup(
            id = UUID.randomUUID().toString(),
            name = "Взять товар",
            order = 1,
            targetFieldType = TaskXLineFieldType.STORAGE_PRODUCT,
            wmsAction = WmsAction.TAKE_FROM,
            resultType = "TaskProduct",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать ячейку хранения",
                    actionType = FactLineXActionType.SELECT_BIN,
                    order = 1,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Отсканируйте или выберите ячейку хранения"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать товар",
                    actionType = FactLineXActionType.SELECT_PRODUCT,
                    order = 2,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Выберите товар из ячейки"
                ),
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Введите количество",
                    actionType = FactLineXActionType.ENTER_QUANTITY,
                    order = 3,
                    promptText = "Введите количество товара"
                )
            )
        )

        // Создаем группу действий "Положить товар"
        val putToGroup = FactLineActionGroup(
            id = UUID.randomUUID().toString(),
            name = "Положить товар",
            order = 2,
            targetFieldType = TaskXLineFieldType.PLACEMENT_BIN,
            wmsAction = WmsAction.PUT_INTO,
            resultType = "BinX",
            actions = listOf(
                FactLineXAction(
                    id = UUID.randomUUID().toString(),
                    name = "Выбрать ячейку размещения",
                    actionType = FactLineXActionType.SELECT_BIN,
                    order = 1,
                    selectionCondition = ObjectSelectionCondition.FROM_PLAN,
                    promptText = "Отсканируйте или выберите ячейку размещения"
                )
            )
        )

        return TaskTypeX(
            id = "8910111213141",
            name = "Перемещение",
            wmsOperation = WmsOperation.MOVEMENT,
            canBeCreatedInApp = false,
            allowCompletionWithoutFactLines = false,
            allowExceedPlanQuantity = false,
            availableActions = listOf(
                AvailableTaskAction.PAUSE,
                AvailableTaskAction.RESUME,
                AvailableTaskAction.SHOW_PLAN_LINES,
                AvailableTaskAction.SHOW_FACT_LINES,
                AvailableTaskAction.COMPARE_LINES
            ),
            factLineActionGroups = listOf(takeFromGroup, putToGroup),
            finalActions = emptyList()
        )
    }

    // Обновим метод createInitialTaskTypes
    private fun createInitialTaskTypes(): Map<String, TaskTypeX> {
        // Создаем тип задания "Приемка по монопалетам" из примера
        val receiptTaskType = createReceiptTaskType()

        // Создаем тип задания "Отбор заказа"
        val pickingTaskType = createPickingTaskType()

        // Создаем тип задания "Перемещение"
        val movementTaskType = createMovementTaskType()

        return mapOf(
            receiptTaskType.id to receiptTaskType,
            pickingTaskType.id to pickingTaskType,
            movementTaskType.id to movementTaskType
        )
    }
}