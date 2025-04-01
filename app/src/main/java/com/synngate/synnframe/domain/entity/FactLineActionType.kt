package com.synngate.synnframe.domain.entity

enum class FactLineActionType {
    ENTER_PRODUCT_ANY,        // Ввести любой товар
    ENTER_PRODUCT_FROM_PLAN,  // Ввести товар из плана
    ENTER_QUANTITY,           // Ввести количество
    ENTER_BIN_ANY,            // Ввести любую ячейку
    ENTER_BIN_FROM_PLAN       // Ввести ячейку из плана
}