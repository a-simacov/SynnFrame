// Товар задания (TaskProduct) - исправленный
package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TaskProduct(
    val product: Product,             // Товар из классификатора
    @Serializable(with = LocalDateTimeSerializer::class)
    val expirationDate: LocalDate = LocalDate.of(1970, 1, 1), // Срок годности (1970-01-01 как индикатор "не установлено")
    val status: ProductStatus = ProductStatus.STANDARD, // Статус
    val quantity: Float = 0f          // Количество (в базовых ЕИ)
) {
    // Проверка, установлен ли срок годности
    fun hasExpirationDate(): Boolean {
        return expirationDate.isAfter(LocalDate.of(1970, 1, 1))
    }
}