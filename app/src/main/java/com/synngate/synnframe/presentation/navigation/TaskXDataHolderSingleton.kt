package com.synngate.synnframe.presentation.navigation

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.buffer.TaskBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Глобальный синглтон для хранения данных задания в памяти.
 * Данные не очищаются при переходе между экранами.
 */
object TaskXDataHolderSingleton {
    private val _currentTask = MutableStateFlow<TaskX?>(null)
    val currentTask: StateFlow<TaskX?> = _currentTask.asStateFlow()

    val currentTaskType: TaskTypeX?
        get() = _currentTask.value?.taskType

    private val _taskBuffer = TaskBuffer()
    val taskBuffer: TaskBuffer get() = _taskBuffer

    private var _endpoint: String? = null
    val endpoint: String? get() = _endpoint

    fun setTaskData(task: TaskX, taskType: TaskTypeX, endpoint: String) {
        Timber.d("TaskXDataHolderSingleton: установка данных задания ${task.id} с типом ${taskType.id}")

        val taskWithType = if (task.taskType == null) {
            task.copy(taskType = taskType)
        } else {
            task
        }

        _currentTask.value = taskWithType
        _endpoint = endpoint
        _taskBuffer.clear()

        Timber.d("TaskXDataHolderSingleton: данные задания установлены, endpoint = $endpoint")
    }

    fun updateTask(task: TaskX) {
        Timber.d("TaskXDataHolderSingleton: обновление задания ${task.id}")
        _currentTask.value = task
    }

    fun addFactAction(factAction: FactAction) {
        Timber.d("TaskXDataHolderSingleton: добавление фактического действия ${factAction.id} к заданию ${factAction.taskId}")
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
        val hasTask = _currentTask.value != null
        val hasEndpoint = _endpoint != null
        Timber.d("TaskXDataHolderSingleton: проверка наличия данных: hasTask=$hasTask, hasEndpoint=$hasEndpoint")
        return hasTask && hasEndpoint
    }

    fun clear() {
        Timber.d("TaskXDataHolderSingleton: очистка данных - вызов отклонен, данные сохраняются")
        // Преднамеренно не очищаем данные при переходе между экранами
        // Очистка должна вызываться только при завершении работы с заданием
    }

    /**
     * Принудительная очистка данных (только для специальных случаев)
     */
    fun forceClean() {
        Timber.d("TaskXDataHolderSingleton: принудительная очистка данных")
        _currentTask.value = null
        _endpoint = null
        _taskBuffer.clear()
    }
}