package com.synngate.synnframe.presentation.navigation

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.buffer.TaskBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Холдер для хранения данных задания в рамках навигационного графа TaskX.
 * Автоматически очищается при выходе из графа навигации.
 */
class TaskXDataHolder {
    private val _currentTask = MutableStateFlow<TaskX?>(null)
    val currentTask: StateFlow<TaskX?> = _currentTask.asStateFlow()

    val currentTaskType: TaskTypeX?
        get() = _currentTask.value?.taskType

    private val _taskBuffer = TaskBuffer()
    val taskBuffer: TaskBuffer get() = _taskBuffer

    private var _endpoint: String? = null
    val endpoint: String? get() = _endpoint

    fun setTaskData(task: TaskX, taskType: TaskTypeX, endpoint: String) {
        // Устанавливаем taskType в task, если он еще не установлен
        val taskWithType = if (task.taskType == null) {
            task.copy(taskType = taskType)
        } else {
            task
        }

        _currentTask.value = taskWithType
        _endpoint = endpoint
        _taskBuffer.clear()
    }

    fun updateTask(task: TaskX) {
        _currentTask.value = task
    }

    fun addFactAction(factAction: FactAction) {
        _currentTask.update { task ->
            task?.let {
                val updatedFactActions = it.factActions + factAction
                val updatedPlannedActions = it.plannedActions.map { plannedAction ->
                    if (plannedAction.id == factAction.plannedActionId) {
                        plannedAction.copy(isCompleted = true)
                    } else {
                        plannedAction
                    }
                }

                it.copy(
                    factActions = updatedFactActions,
                    plannedActions = updatedPlannedActions
                )
            }
        }
    }

    fun hasData(): Boolean {
        return _currentTask.value != null && _endpoint != null
    }

    fun clear() {
        _currentTask.value = null
        _endpoint = null
        _taskBuffer.clear()
    }
}