package com.synngate.synnframe.domain.entity.taskx.action

enum class ActionObjectType {
    CLASSIFIER_PRODUCT, // Товар из классификатора
    TASK_PRODUCT,      // Товар задания
    PRODUCT_QUANTITY,  // Количество товара (новый тип)
    PALLET,           // Паллета
    BIN               // Ячейка
}