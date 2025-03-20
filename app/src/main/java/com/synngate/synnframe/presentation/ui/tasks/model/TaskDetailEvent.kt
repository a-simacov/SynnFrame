package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.TaskFactLine

/**
 * События для экрана деталей задания
 */
sealed class TaskDetailEvent {
    // Навигация назад
    data object NavigateBack : TaskDetailEvent()

    // Навигация к экрану списка товаров
    data object NavigateToProductsList : TaskDetailEvent()

    // Показать снэкбар с сообщением
    data class ShowSnackbar(val message: String) : TaskDetailEvent()

    // Показать диалог сканирования штрихкодов
    data object ShowScanDialog : TaskDetailEvent()

    // Показать диалог редактирования строки факта
    data class ShowFactLineDialog(val factLine: TaskFactLine) : TaskDetailEvent()

    // Закрыть диалог
    data object CloseDialog : TaskDetailEvent()

    // Обновление успешно
    data object UpdateSuccess : TaskDetailEvent()
}