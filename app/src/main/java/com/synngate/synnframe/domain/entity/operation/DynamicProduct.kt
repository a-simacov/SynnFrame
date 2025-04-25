package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.Serializable

/**
 * Заготовка для модели динамического товара.
 * Будет расширена в будущем при реализации функционала работы с товарами.
 */
@Serializable
data class DynamicProduct(
    val id: String,
    val name: String,
    // Другие свойства будут добавлены при реализации работы с товарами
)