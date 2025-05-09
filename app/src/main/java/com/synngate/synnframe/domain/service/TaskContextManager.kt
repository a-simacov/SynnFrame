package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.TaskXStartResponseDto
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskContextManager {

    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<String?>(null)
    val currentEndpoint: StateFlow<String?> = _currentEndpoint.asStateFlow()

    fun saveStartedTask(response: TaskXStartResponseDto, endpoint: String) {
        val processedTask = processTaskActions(response.task)

        _lastStartedTaskX.value = processedTask
        _lastTaskTypeX.value = response.taskType
        _currentEndpoint.value = endpoint
    }

    fun updateTask(updatedTask: TaskX, skipStatusProcessing: Boolean = false) {
        if (_lastStartedTaskX.value?.id == updatedTask.id) {
            val processedTask = if (skipStatusProcessing) updatedTask else processTaskActions(updatedTask)

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

        val updatedPlannedActions = task.plannedActions.map { plannedAction ->
            // Используем метод isActionCompleted для корректного определения завершенности
            // Этот метод учитывает:
            // 1. Наличие флага isCompleted
            // 2. Наличие флага manuallyCompleted
            // 3. Для действий с учетом количества - сравнение планового и фактического количества
            val isActionActuallyCompleted = plannedAction.isActionCompleted(task.factActions)

            // Обновляем статус только если текущий статус не соответствует фактическому
            // Это позволяет сохранять ручные изменения статуса
            if (isActionActuallyCompleted != plannedAction.isCompleted) {
                plannedAction.copy(isCompleted = isActionActuallyCompleted)
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