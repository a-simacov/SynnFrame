package com.synngate.synnframe.presentation.ui.tasks.model

enum class ScanningState {
    IDLE,           // Начальное состояние
    SCAN_PRODUCT,   // Сканирование товара
    SCAN_BIN,       // Сканирование ячейки
    ENTER_QUANTITY, // Ввод количества
    CONFIRM         // Подтверждение
}