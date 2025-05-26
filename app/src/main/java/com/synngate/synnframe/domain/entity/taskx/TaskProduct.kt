package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TaskProduct(
    val id: String,
    val product: Product,
    @Serializable(with = LocalDateTimeSerializer::class)
    val expirationDate: LocalDateTime? = null,
    val status: ProductStatus = ProductStatus.STANDARD
) {

    fun hasExpirationDate(): Boolean {
        return expirationDate != null && expirationDate.isAfter(LocalDateTime.of(1970, 1, 1, 0, 0))
    }
}