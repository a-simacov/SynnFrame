package com.synngate.synnframe.domain.entity.taskx

enum class TaskXLineFieldType {
    STORAGE_PRODUCT,      // Товар хранения
    STORAGE_PALLET,       // Паллета хранения
    PLACEMENT_PALLET,     // Паллета размещения
    PLACEMENT_BIN,
    WMS_ACTION;        // Ячейка размещения
}