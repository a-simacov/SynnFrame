package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine

data class TaskLineItem(
    val planLine: TaskPlanLine,
    val factLine: TaskFactLine?,
    val product: Product?,
    val binName: String? = null
)

data class TaskDetailState(
    // Основные данные задания
    val taskId: String = "",
    val task: Task? = null,
    val taskLines: List<TaskLineItem> = emptyList(),

    // Данные для ввода
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isEditable: Boolean = false,

    // Диалоги
    val isScanDialogVisible: Boolean = false,
    val isFactLineDialogVisible: Boolean = false,
    val isCompleteConfirmationVisible: Boolean = false,
    val selectedFactLine: TaskFactLine? = null,
    val selectedPlanQuantity: Float = 0f,

    // Состояние ввода строки факта
    val isEntryActive: Boolean = false,  // Признак активности ввода строки факта
    val entryStep: EntryStep = EntryStep.NONE,  // Текущий шаг ввода
    val entryBinCode: String? = null,    // Введенный код ячейки
    val entryBinName: String? = null,    // Форматированное имя ячейки
    val entryProduct: Product? = null,   // Выбранный товар
    val entryQuantity: Float? = null     // Введенное количество
)

// Четкое перечисление шагов ввода
enum class EntryStep {
    NONE,       // Нет активного ввода
    ENTER_BIN,  // Ввод ячейки
    ENTER_PRODUCT, // Ввод товара
    ENTER_QUANTITY // Ввод количества
}