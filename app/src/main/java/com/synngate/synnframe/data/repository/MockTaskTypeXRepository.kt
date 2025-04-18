package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.taskx.AvailableTaskAction
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.WmsOperation
import com.synngate.synnframe.domain.repository.ActionTemplateRepository
import com.synngate.synnframe.domain.repository.TaskTypeXRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockTaskTypeXRepository(
    private val actionTemplateRepository: ActionTemplateRepository
) : TaskTypeXRepository {

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
        addTaskType(taskType)
    }

    override suspend fun deleteTaskType(id: String) {
        val updatedMap = taskTypesFlow.value.toMutableMap()
        updatedMap.remove(id)
        taskTypesFlow.value = updatedMap
    }

    // Создание типов заданий
    private fun createInitialTaskTypes(): Map<String, TaskTypeX> {
        val taskTypes = mutableMapOf<String, TaskTypeX>()

        // Тип задания "Перемещение паллеты" из примера
        val movePalletTaskType = TaskTypeX(
            id = "task_type_move_pallet",
            name = "Перемещение паллеты",
            wmsOperation = WmsOperation.PLACEMENT,
            allowCompletionWithoutFactActions = false,
            availableActions = listOf(
                AvailableTaskAction.PAUSE,
                AvailableTaskAction.RESUME,
                AvailableTaskAction.SHOW_PLAN_LINES,
                AvailableTaskAction.SHOW_FACT_LINES,
                AvailableTaskAction.COMPARE_LINES
            )
        )

        // Добавляем типы заданий в результирующую карту
        taskTypes[movePalletTaskType.id] = movePalletTaskType

        return taskTypes
    }
}