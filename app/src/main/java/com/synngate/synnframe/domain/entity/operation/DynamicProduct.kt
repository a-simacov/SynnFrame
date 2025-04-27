package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.AccountingModel
import kotlinx.serialization.Serializable

interface DynamicProduct {

    fun getId(): String

    fun getName(): String

    fun getAccountingModelString(): String

    fun getAccountingModelEnum(): AccountingModel

    fun getArticleNumber(): String

    fun getMainUnitId(): String

    fun getUnits(): List<DynamicProductUnit>

    fun getMainUnit(): DynamicProductUnit?

    fun getAllBarcodes(): List<String>

    object Empty : DynamicProduct {
        override fun getId(): String = ""
        override fun getName(): String = ""
        override fun getAccountingModelString(): String = "QTY"
        override fun getAccountingModelEnum(): AccountingModel = AccountingModel.QTY
        override fun getArticleNumber(): String = ""
        override fun getMainUnitId(): String = ""
        override fun getUnits(): List<DynamicProductUnit> = emptyList()
        override fun getMainUnit(): DynamicProductUnit? = null
        override fun getAllBarcodes(): List<String> = emptyList()
    }

    @Serializable
    data class Base(
        private val id: String,
        private val name: String,
        private val accountingModel: String,
        private val articleNumber: String,
        private val mainUnitId: String,
        private val units: List<DynamicProductUnit> = emptyList()
    ) : DynamicProduct {

        override fun getId(): String = id
        override fun getName(): String = name
        override fun getAccountingModelString(): String = accountingModel

        override fun getAccountingModelEnum(): AccountingModel {
            return when (accountingModel.uppercase()) {
                "QTY" -> AccountingModel.QTY
                "BATCH" -> AccountingModel.BATCH
                else -> AccountingModel.QTY
            }
        }

        override fun getArticleNumber(): String = articleNumber
        override fun getMainUnitId(): String = mainUnitId
        override fun getUnits(): List<DynamicProductUnit> = units

        override fun getMainUnit(): DynamicProductUnit? =
            units.find { it.id == mainUnitId }

        override fun getAllBarcodes(): List<String> =
            units.flatMap { unit -> listOf(unit.mainBarcode) + unit.barcodes }.distinct()
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