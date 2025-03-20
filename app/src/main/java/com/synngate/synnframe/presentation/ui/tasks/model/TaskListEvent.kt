package com.synngate.synnframe.presentation.ui.tasks.model

import java.time.LocalDateTime


/**
 * События для экрана списка заданий
 */
sealed class TaskListEvent {
    // Навигация к экрану деталей задания
    data class NavigateToTaskDetail(val taskId: String) : TaskListEvent()

    // Показать снэкбар с сообщением
    data class ShowSnackbar(val message: String) : TaskListEvent()

    // Показать диалог выбора даты для фильтра
    data class ShowDatePicker(val isFromDate: Boolean, val currentDate: LocalDateTime?) : TaskListEvent()
}