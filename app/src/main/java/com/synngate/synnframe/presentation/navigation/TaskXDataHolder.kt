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

    private val _currentTaskType = MutableStateFlow<TaskTypeX?>(null)
    val currentTaskType: StateFlow<TaskTypeX?> = _currentTaskType.asStateFlow()

    private val _taskBuffer = TaskBuffer()
    val taskBuffer: TaskBuffer get() = _taskBuffer

    private var _endpoint: String? = null
    val endpoint: String? get() = _endpoint

    /**
     * Устанавливает данные задания при входе в граф навигации
     */
    fun setTaskData(task: TaskX, taskType: TaskTypeX, endpoint: String) {
        _currentTask.value = task
        _currentTaskType.value = taskType
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

    fun getTaskData(): Pair<TaskX, TaskTypeX>? {
        val task = _currentTask.value
        val taskType = _currentTaskType.value

        return if (task != null && taskType != null) {
            Pair(task, taskType)
        } else {
            null
        }
    }

    fun hasData(): Boolean {
        return _currentTask.value != null && _currentTaskType.value != null
    }

    fun clear() {
        _currentTask.value = null
        _currentTaskType.value = null
        _endpoint = null
        _taskBuffer.clear()
    }
}