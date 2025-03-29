package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.Product

/**
 * Перечисление свойств товара, которые могут быть отображены в интерфейсе
 */
enum class ProductPropertyType {
    NAME,           // Наименование товара
    ID,             // Идентификатор товара
    ARTICLE,        // Артикул товара
    UNIT_NAME,      // Наименование единицы измерения
    MAIN_BARCODE,   // Основной штрихкод
    MODEL           // Модель учёта
}

/**
 * Класс, определяющий как отображать свойство товара
 * @param type Тип свойства товара
 * @param label Метка перед значением (опционально)
 * @param prefix Префикс перед значением (опционально)
 * @param suffix Суффикс после значения (опционально)
 */
data class ProductDisplayProperty(
    val type: ProductPropertyType,
    val label: String? = null,
    val prefix: String? = null,
    val suffix: String? = null
)

/**
 * Функция для получения значения свойства товара по его типу
 * @param product Товар
 * @param type Тип свойства
 * @return Значение свойства или пустая строка, если свойство не доступно
 */
fun getProductPropertyValue(product: Product?, type: ProductPropertyType): String {
    if (product == null) return ""

    return when (type) {
        ProductPropertyType.NAME -> product.name
        ProductPropertyType.ID -> product.id
        ProductPropertyType.ARTICLE -> product.articleNumber
        ProductPropertyType.MODEL -> product.accountingModel.name
        ProductPropertyType.UNIT_NAME -> {
            val mainUnit = product.units.find { it.id == product.mainUnitId }
            mainUnit?.name ?: ""
        }
        ProductPropertyType.MAIN_BARCODE -> {
            val mainUnit = product.units.find { it.id == product.mainUnitId }
            mainUnit?.mainBarcode ?: ""
        }
    }
}

/**
 * Функция для форматирования свойства товара с учетом метки, префикса и суффикса
 * @param product Товар
 * @param property Настройка отображения свойства
 * @return Отформатированная строка свойства
 */
fun formatProductProperty(product: Product?, property: ProductDisplayProperty): String {
    val value = getProductPropertyValue(product, property.type)
    if (value.isEmpty()) return ""

    val label = property.label?.let { "$it: " } ?: ""
    val prefix = property.prefix ?: ""
    val suffix = property.suffix ?: ""

    return "$label$prefix$value$suffix"
}