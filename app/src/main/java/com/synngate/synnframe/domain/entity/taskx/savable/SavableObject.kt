package com.synngate.synnframe.domain.entity.taskx.savable

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

/**
 * Сохраняемый объект в контексте выполнения задания
 * Используется для автоматического заполнения полей в последующих действиях
 */
@Serializable
data class SavableObject(
    val id: String = UUID.randomUUID().toString(),
    val objectType: ActionObjectType,
    val objectData: SavableObjectData,
    @Serializable(with = LocalDateTimeSerializer::class)
    val savedAt: LocalDateTime = LocalDateTime.now(),
    // Источник объекта (имя шага или действия, откуда был получен объект)
    val source: String
) {
    fun getShortDescription(): String {
        return when (objectData) {
            is SavableObjectData.PalletData -> "Паллета: ${objectData.pallet.code}"
            is SavableObjectData.BinData -> "Ячейка: ${objectData.bin.code}"
            is SavableObjectData.TaskProductData -> "Товар: ${objectData.taskProduct.product.name}"
            is SavableObjectData.ProductData -> "Товар: ${objectData.product.name}"
        }
    }
}

@Serializable
sealed class SavableObjectData {

    @Serializable
    data class PalletData(val pallet: Pallet) : SavableObjectData()

    @Serializable
    data class BinData(val bin: BinX) : SavableObjectData()

    @Serializable
    data class TaskProductData(val taskProduct: TaskProduct) : SavableObjectData()

    @Serializable
    data class ProductData(val product: Product) : SavableObjectData()

    fun extractData(): Any {
        return when (this) {
            is PalletData -> pallet
            is BinData -> bin
            is TaskProductData -> taskProduct
            is ProductData -> product
        }
    }
}

fun createSavableObjectData(objectType: ActionObjectType, data: Any): SavableObjectData? {
    return when (objectType) {
        ActionObjectType.PALLET -> {
            if (data is Pallet) {
                SavableObjectData.PalletData(data)
            } else null
        }
        ActionObjectType.BIN -> {
            if (data is BinX) {
                SavableObjectData.BinData(data)
            } else null
        }
        ActionObjectType.TASK_PRODUCT -> {
            if (data is TaskProduct) {
                SavableObjectData.TaskProductData(data)
            } else null
        }
        ActionObjectType.CLASSIFIER_PRODUCT -> {
            if (data is Product) {
                SavableObjectData.ProductData(data)
            } else null
        }
        else -> null
    }
}