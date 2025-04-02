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
    // ОСНОВНЫЕ ДАННЫЕ ЗАДАНИЯ
    val taskId: String = "",                       // ID задания
    val task: Task? = null,                        // Полные данные задания
    val taskLines: List<TaskLineItem> = emptyList(), // Комбинированные строки плана и факта

    // СОСТОЯНИЯ ЗАГРУЗКИ И ОШИБОК
    val isLoading: Boolean = false,                // Загрузка данных задания
    val isProcessing: Boolean = false,             // Выполнение операции
    val error: String? = null,                     // Сообщение об ошибке
    val isEditable: Boolean = false,               // Можно ли редактировать задание

    // ПОИСКОВЫЙ ЗАПРОС
    val searchQuery: String = "",                  // Текущий поисковый запрос в поле ввода

    // СОСТОЯНИЕ ПРОЦЕССА ВВОДА СТРОКИ ФАКТА
    val isEntryActive: Boolean = false,            // Активен ли процесс ввода строки факта
    val entryStep: EntryStep = EntryStep.NONE,     // Текущий шаг ввода

    // ВРЕМЕННЫЕ ДАННЫЕ СТРОКИ ФАКТА
    val entryBinCode: String? = null,              // Введенный код ячейки
    val entryBinName: String? = null,              // Форматированное имя ячейки
    val entryProduct: Product? = null,             // Выбранный товар
    val entryQuantity: Float? = null,              // Введенное количество

    // СОСТОЯНИЕ ДИАЛОГА ВВОДА КОЛИЧЕСТВА
    val isFactLineDialogVisible: Boolean = false,  // Показан ли диалог ввода количества
    val selectedFactLine: TaskFactLine? = null,     // Выбранная строка факта
    val selectedPlanQuantity: Float = 0f,          // Плановое количество
    val factLineDialogState: FactLineDialogState = FactLineDialogState(), // Состояние диалога

    // СОСТОЯНИЕ ДРУГИХ ДИАЛОГОВ
    val isScanDialogVisible: Boolean = false,      // Показан ли диалог сканирования
    val isCompleteConfirmationVisible: Boolean = false, // Показан ли диалог подтверждения
    val scanBarcodeDialogState: ScanBarcodeDialogState = ScanBarcodeDialogState(), // Состояние диалога сканирования

    // Порядок ввода строки факта
    val entrySequence: List<EntryStep> = emptyList(),   // Полная последовательность шагов
    val entryStepIndex: Int = 0,                        // Индекс текущего шага

    // Флаг для сохранения контекста при навигации
    val pendingProductSelection: Boolean = false,     // Ожидается выбор товара
    val pendingReturnStep: EntryStep? = null,

    val isProductSelectionDialogVisible: Boolean = false,  // Флаг отображения диалога
    val productSelectionFilter: String = "",               // Текущий фильтр поиска товаров
    val filteredProducts: List<Product> = emptyList(),     // Отфильтрованный список товаров
    val isProductsLoading: Boolean = false,                // Флаг загрузки списка товаров
    val planProductIds: Set<String>? = null
)

// Четкое перечисление шагов ввода
enum class EntryStep {
    NONE,       // Нет активного ввода
    ENTER_BIN,  // Ввод ячейки
    ENTER_PRODUCT, // Ввод товара
    ENTER_QUANTITY // Ввод количества
}