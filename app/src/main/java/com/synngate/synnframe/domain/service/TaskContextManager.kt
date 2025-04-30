package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.TaskXStartResponseDto
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class TaskContextManager {

    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    fun saveStartedTask(response: TaskXStartResponseDto) {
        _lastStartedTaskX.value = response.task
        _lastTaskTypeX.value = response.taskType
    }

    fun updateTask(updatedTask: TaskX) {
        if (_lastStartedTaskX.value?.id == updatedTask.id) {
            _lastStartedTaskX.value = updatedTask
        } else {
            Timber.w("Попытка обновить задание, которого нет в контексте: ${updatedTask.id}")
        }
    }

    fun clearContext() {
        _lastStartedTaskX.value = null
        _lastTaskTypeX.value = null
    }
}