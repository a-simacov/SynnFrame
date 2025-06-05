package com.synngate.synnframe.domain.entity

import com.synngate.synnframe.domain.entity.AccountingModel.BATCH
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val accountingModel: AccountingModel = AccountingModel.QTY,
    val articleNumber: String = "",
    val mainUnitId: String = "",
    val weight: Float = 0.0f,
    val units: List<ProductUnit> = emptyList()
) {

    fun getMainUnit(): ProductUnit? = units.find { it.id == mainUnitId }

    fun getAllBarcodes(): List<String> = units.flatMap { it.barcodes }

    fun usesExpDate(): Boolean = (accountingModel == BATCH)
}

@Serializable
data class ProductUnit(
    val id: String,
    val productId: String,
    val name: String,
    val quantity: Float,
    val mainBarcode: String,
    val barcodes: List<String> = emptyList()
) {
    val allBarcodes: List<String>
        get() = listOf(mainBarcode) + barcodes.distinct()
}