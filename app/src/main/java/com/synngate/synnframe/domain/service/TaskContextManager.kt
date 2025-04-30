package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.TaskXStartResponseDto
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Менеджер контекста заданий для обмена данными между различными экранами заданий
 */
class TaskContextManager {

    // Хранение последнего запущенного задания TaskX
    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    // Хранение последнего типа задания
    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    fun saveStartedTask(response: TaskXStartResponseDto) {
        _lastStartedTaskX.value = response.task
        _lastTaskTypeX.value = response.taskType
        Timber.d("Сохранен контекст запущенного задания: ${response.task.id}")
    }

    fun clearContext() {
        _lastStartedTaskX.value = null
        _lastTaskTypeX.value = null
        Timber.d("Контекст задания очищен")
    }
}