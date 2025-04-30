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
 * Расширенная версия с поддержкой локального обновления задания
 */
class TaskContextManager {

    // Хранение последнего запущенного задания TaskX
    private val _lastStartedTaskX = MutableStateFlow<TaskX?>(null)
    val lastStartedTaskX: StateFlow<TaskX?> = _lastStartedTaskX.asStateFlow()

    // Хранение последнего типа задания
    private val _lastTaskTypeX = MutableStateFlow<TaskTypeX?>(null)
    val lastTaskTypeX: StateFlow<TaskTypeX?> = _lastTaskTypeX.asStateFlow()

    /**
     * Сохранение задания, полученного с сервера
     */
    fun saveStartedTask(response: TaskXStartResponseDto) {
        _lastStartedTaskX.value = response.task
        _lastTaskTypeX.value = response.taskType
        Timber.d("Сохранен контекст запущенного задания: ${response.task.id}")
    }

    /**
     * Обновление существующего задания в контексте
     * (используется для локального отслеживания изменений состояния задания)
     */
    fun updateTask(updatedTask: TaskX) {
        if (_lastStartedTaskX.value?.id == updatedTask.id) {
            _lastStartedTaskX.value = updatedTask
            Timber.d("Обновлен контекст задания: ${updatedTask.id}")
        } else {
            Timber.w("Попытка обновить задание, которого нет в контексте: ${updatedTask.id}")
        }
    }

    /**
     * Очистка контекста задания
     */
    fun clearContext() {
        _lastStartedTaskX.value = null
        _lastTaskTypeX.value = null
        Timber.d("Контекст задания очищен")
    }
}