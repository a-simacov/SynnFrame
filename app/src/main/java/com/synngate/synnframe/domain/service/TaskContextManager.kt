package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.TaskXStartResponseDto
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class TaskContextManager {

    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<String?>(null)
    val currentEndpoint: StateFlow<String?> = _currentEndpoint.asStateFlow()

    private val savableObjectsManager: SavableObjectService = SavableObjectsManager()
    val savableObjects = savableObjectsManager.objects

    fun saveStartedTask(response: TaskXStartResponseDto, endpoint: String) {
        val processedTask = processTaskActions(response.task)

        _lastStartedTaskX.value = processedTask
        _lastTaskTypeX.value = response.taskType
        _currentEndpoint.value = endpoint

        savableObjectsManager.clear()
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
            val isActionActuallyCompleted = plannedAction.isActionCompleted(task.factActions)

            if (isActionActuallyCompleted != plannedAction.isCompleted) {
                plannedAction.copy(isCompleted = isActionActuallyCompleted)
            } else {
                plannedAction
            }
        }

        return task.copy(plannedActions = updatedPlannedActions)
    }

    fun addSavableObject(objectType: ActionObjectType, data: Any, source: String): Boolean {
        val taskType = _lastTaskTypeX.value
        if (taskType == null) {
            Timber.w("Невозможно добавить сохраняемый объект: тип задания не определен")
            return false
        }

        if (!taskType.savableObjectTypes.contains(objectType)) {
            Timber.w("Тип объекта $objectType не входит в список разрешенных для сохранения")
            return false
        }

        return savableObjectsManager.addObject(objectType, data, source)
    }

    fun removeSavableObject(id: String): Boolean {
        return savableObjectsManager.removeObject(id)
    }

    fun clearSavableObjects() {
        savableObjectsManager.clear()
    }

    fun <T : Any> getSavableObjectData(type: ActionObjectType): T? {
        return savableObjectsManager.getDataByType(type)
    }

    fun hasSavableObjectOfType(type: ActionObjectType): Boolean {
        val taskType = _lastTaskTypeX.value
        return taskType?.savableObjectTypes?.contains(type) == true &&
                savableObjectsManager.hasObjectOfType(type)
    }
}