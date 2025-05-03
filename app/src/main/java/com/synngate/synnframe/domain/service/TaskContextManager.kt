package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.TaskXStartResponseDto
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TaskContextManager {

    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<String?>(null)
    val currentEndpoint: StateFlow<String?> = _currentEndpoint.asStateFlow()

    fun saveStartedTask(response: TaskXStartResponseDto, endpoint: String) {
        // Объединяем обычные и финальные действия
        val allPlannedActions = response.task.plannedActions.toMutableList()

        // Создаем плановые действия из шаблонов финальных действий, если они есть
        val finalPlannedActions = response.taskType.finalActions?.map { template ->
            PlannedAction(
                id = UUID.randomUUID().toString(),
                // Высокий порядковый номер ставит финальные действия в конец списка
                order = 1000 + allPlannedActions.size,
                actionTemplate = template,
                wmsAction = template.wmsAction,
                isFinalAction = true // Маркируем как финальные действия
            )
        } ?: emptyList()

        // Добавляем финальные действия в общий список
        allPlannedActions.addAll(finalPlannedActions)

        // Обновляем задание с полным списком действий
        val taskWithAllActions = response.task.copy(
            plannedActions = allPlannedActions
        )

        val processedTask = processTaskActions(taskWithAllActions)

        _lastStartedTaskX.value = processedTask
        _lastTaskTypeX.value = response.taskType
        _currentEndpoint.value = endpoint
    }

    fun updateTask(updatedTask: TaskX) {
        if (_lastStartedTaskX.value?.id == updatedTask.id) {
            val processedTask = processTaskActions(updatedTask)
            // Используем двухэтапное обновление для гарантии оповещения подписчиков
            // Сначала устанавливаем null, чтобы форсировать обновление даже при равных ссылках
            _lastStartedTaskX.value = null
            // Затем устанавливаем новое значение
            _lastStartedTaskX.value = processedTask
        }
    }

    private fun processTaskActions(task: TaskX): TaskX {
        if (task.factActions.isEmpty()) {
            return task
        }

        val factActionsByPlannedId = task.factActions
            .filter { it.plannedActionId != null }
            .groupBy { it.plannedActionId!! }

        val updatedPlannedActions = task.plannedActions.map { plannedAction ->
            val hasFactAction = factActionsByPlannedId.containsKey(plannedAction.id)
            if (hasFactAction && !plannedAction.isCompleted) {
                plannedAction.copy(isCompleted = true)
            } else {
                plannedAction
            }
        }

        return task.copy(plannedActions = updatedPlannedActions)
    }

    fun clearContext() {
        _lastStartedTaskX.value = null
        _lastTaskTypeX.value = null
        _currentEndpoint.value = null
    }
}