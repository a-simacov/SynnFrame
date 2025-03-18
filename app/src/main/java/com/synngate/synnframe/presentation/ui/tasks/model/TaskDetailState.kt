package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus

/**
 * Комбинированные данные строки плана и факта задания
 */
data class TaskLineItem(
    val planLine: TaskPlanLine,
    val factLine: TaskFactLine?,
    val product: Product?
)

/**
 * Состояние экрана деталей задания
 */
data class TaskDetailState(
    // Идентификатор задания
    val taskId: String = "",

    // Задание
    val task: Task? = null,

    // Комбинированные строки плана и факта
    val taskLines: List<TaskLineItem> = emptyList(),

    // Поисковый запрос для товаров
    val searchQuery: String = "",

    // Распознанный штрихкод
    val scannedBarcode: String? = null,

    // Найденный по штрихкоду товар
    val scannedProduct: Product? = null,

    // Выбранная строка факта для редактирования
    val selectedFactLine: TaskFactLine? = null,

    // Дополнительное количество для строки факта
    val additionalQuantity: String = "",

    // Видимость диалога сканирования
    val isScanDialogVisible: Boolean = false,

    // Видимость диалога редактирования строки факта
    val isFactLineDialogVisible: Boolean = false,

    // Видимость диалога подтверждения завершения задания
    val isCompleteConfirmationVisible: Boolean = false,

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Операция в процессе
    val isProcessing: Boolean = false,

    // Ошибка (если есть)
    val error: String? = null,

    // Признак доступности изменения задания
    val isEditable: Boolean = false
)

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