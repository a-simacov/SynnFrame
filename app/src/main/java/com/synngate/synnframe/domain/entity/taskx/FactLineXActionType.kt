package com.synngate.synnframe.domain.entity.taskx

enum class FactLineXActionType {
    SELECT_PRODUCT,          // Выбрать товар
    ENTER_QUANTITY,          // Ввести количество
    SELECT_BIN,              // Выбрать ячейку
    SELECT_PALLET,           // Выбрать паллету
    CREATE_PALLET,           // Создать паллету
    CLOSE_PALLET,            // Закрыть паллету
    PRINT_LABEL;             // Печать этикетки
}