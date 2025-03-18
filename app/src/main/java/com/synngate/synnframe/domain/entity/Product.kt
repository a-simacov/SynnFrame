package com.synngate.synnframe.domain.entity

/**
 * Доменная модель товара
 */
data class Product(
    /**
     * Идентификатор товара
     */
    val id: String,

    /**
     * Наименование товара
     */
    val name: String,

    /**
     * Модель учёта (по партиям и количеству, только по количеству)
     */
    val accountingModel: AccountingModel,

    /**
     * Артикул
     */
    val articleNumber: String,

    /**
     * Идентификатор основной единицы измерения
     */
    val mainUnitId: String,

    /**
     * Список единиц измерения товара
     */
    val units: List<ProductUnit> = emptyList()
) {
    /**
     * Получение основной единицы измерения
     */
    fun getMainUnit(): ProductUnit? = units.find { it.id == mainUnitId }

    /**
     * Получение всех штрихкодов товара
     */
    fun getAllBarcodes(): List<String> = units.flatMap { unit ->
        unit.allBarcodes
    }

    /**
     * Поиск единицы измерения по штрихкоду
     */
    fun findUnitByBarcode(barcode: String): ProductUnit? = units.find { unit ->
        unit.allBarcodes.contains(barcode)
    }
}

/**
 * Единица измерения товара
 */
data class ProductUnit(
    /**
     * Идентификатор единицы измерения
     */
    val id: String,

    /**
     * Идентификатор товара
     */
    val productId: String,

    /**
     * Наименование единицы измерения
     */
    val name: String,

    /**
     * Коэффициент пересчета к базовой единице
     */
    val quantity: Float,

    /**
     * Основной штрихкод
     */
    val mainBarcode: String,

    /**
     * Список дополнительных штрихкодов
     */
    val barcodes: List<String> = emptyList()
) {
    /**
     * Все штрихкоды единицы измерения (основной + дополнительные)
     */
    val allBarcodes: List<String>
        get() = listOf(mainBarcode) + barcodes
}