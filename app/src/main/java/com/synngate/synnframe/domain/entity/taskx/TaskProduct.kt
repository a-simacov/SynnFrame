package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TaskProduct(
    val product: Product,
    @Serializable(with = LocalDateTimeSerializer::class)
    val expirationDate: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0),
    val status: ProductStatus = ProductStatus.STANDARD,
    val quantity: Float = 0f
) {

    fun hasExpirationDate(): Boolean {
        return expirationDate.isAfter(LocalDateTime.of(1970, 1, 1, 0, 0))
    }
}