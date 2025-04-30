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

    // Добавляем хранение endpoint'а
    private val _currentEndpoint = MutableStateFlow<String?>(null)
    val currentEndpoint: StateFlow<String?> = _currentEndpoint.asStateFlow()

    fun saveStartedTask(response: TaskXStartResponseDto, endpoint: String) {
        _lastStartedTaskX.value = response.task
        _lastTaskTypeX.value = response.taskType
        _currentEndpoint.value = endpoint
        Timber.d("Saved task with endpoint: $endpoint")
    }

    fun updateTask(updatedTask: TaskX) {
        if (_lastStartedTaskX.value?.id == updatedTask.id) {
            // Используем двухэтапное обновление для гарантии оповещения подписчиков
            // Сначала устанавливаем null, чтобы форсировать обновление даже при равных ссылках
            _lastStartedTaskX.value = null
            // Затем устанавливаем новое значение
            _lastStartedTaskX.value = updatedTask
        } else {
            Timber.w("Попытка обновить задание, которого нет в контексте: ${updatedTask.id}")
        }
    }

    fun setEndpoint(endpoint: String) {
        _currentEndpoint.value = endpoint
        Timber.d("Set endpoint: $endpoint")
    }

    fun clearContext() {
        _lastStartedTaskX.value = null
        _lastTaskTypeX.value = null
        _currentEndpoint.value = null
    }
}