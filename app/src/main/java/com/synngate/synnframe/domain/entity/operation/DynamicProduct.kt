package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.AccountingModel
import kotlinx.serialization.Serializable

@Serializable
data class DynamicProduct(
    val id: String,
    val name: String,
    val accountingModel: String,  // Используем String для совместимости с сериализацией
    val articleNumber: String,
    val mainUnitId: String,
    val units: List<DynamicProductUnit> = emptyList()
) {
    fun getMainUnit(): DynamicProductUnit? = units.find { it.id == mainUnitId }

    fun getAllBarcodes(): List<String> = units.flatMap { unit ->
        listOf(unit.mainBarcode) + unit.barcodes
    }.distinct()

    fun getAccountingModelEnum(): AccountingModel {
        return when(accountingModel.uppercase()) {
            "QTY" -> AccountingModel.QTY
            "BATCH" -> AccountingModel.BATCH
            else -> AccountingModel.QTY
        }
    }
}

@Serializable
data class DynamicProductUnit(
    val id: String,
    val productId: String,
    val name: String,
    val quantity: Float,
    val mainBarcode: String,
    val barcodes: List<String> = emptyList()
)