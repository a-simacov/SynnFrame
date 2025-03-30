package com.synngate.synnframe.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val accountingModel: AccountingModel,
    val articleNumber: String,
    val mainUnitId: String,
    val units: List<ProductUnit> = emptyList()
) {

    fun getMainUnit(): ProductUnit? = units.find { it.id == mainUnitId }

    fun getAllBarcodes(): List<String> = units.flatMap { it.barcodes }

    fun findUnitByBarcode(barcode: String): ProductUnit? = units.find { unit ->
        unit.allBarcodes.contains(barcode)
    }
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