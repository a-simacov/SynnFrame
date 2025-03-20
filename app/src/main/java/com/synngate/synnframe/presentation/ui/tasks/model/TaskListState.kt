package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import java.time.LocalDateTime

/**
 * Состояние экрана списка заданий
 */
data class TaskListState(
    // Список заданий
    val tasks: List<Task> = emptyList(),

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Ошибка (если есть)
    val error: String? = null,

    // Поисковый запрос
    val searchQuery: String = "",

    // Выбранные статусы заданий для фильтрации
    val selectedStatusFilters: Set<TaskStatus> = emptySet(),

    // Выбранный тип заданий для фильтрации
    val selectedTypeFilter: TaskType? = null,

    // Дата начала периода для фильтрации
    val dateFromFilter: LocalDateTime? = null,

    // Дата окончания периода для фильтрации
    val dateToFilter: LocalDateTime? = null,

    // Признак отображения панели фильтров
    val isFilterPanelVisible: Boolean = false,

    // Признак синхронизации с сервером
    val isSyncing: Boolean = false,

    // Время последней синхронизации
    val lastSyncTime: String? = null,

    // Общее количество заданий
    val tasksCount: Int = 0
)