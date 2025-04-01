package com.synngate.synnframe.domain.entity

enum class TaskAction {
    PUT_INTO,     // Положить в
    TAKE_FROM,    // Извлечь из
    RECEIPT,      // Оприходовать
    EXPENSE,      // Списать
    RECOUNT,      // Пересчитать
    PACK,         // Упаковать
    VERIFY,       // Проверить
    MARKING       // Маркировать
}