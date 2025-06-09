package com.synngate.synnframe.presentation.ui.products.mapper

import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.ui.products.model.BarcodeUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductListItemUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductUnitUiModel
import com.synngate.synnframe.util.format.QuantityFormatter
import com.synngate.synnframe.util.resources.ResourceProvider

class ProductUiMapper(
    private val resourceProvider: ResourceProvider
) {
    fun mapToListItem(
        product: Product,
        isSelected: Boolean = false
    ): ProductListItemUiModel {
        val mainUnit = product.getMainUnit()

        return ProductListItemUiModel(
            id = product.id,
            name = product.name,
            articleText = resourceProvider.getString(
                R.string.product_article,
                product.articleNumber
            ),
            mainUnitText = resourceProvider.getString(
                R.string.product_main_unit,
                mainUnit?.name ?: ""
            ),
            isSelected = isSelected
        )
    }

    fun mapToDetailModel(product: Product): ProductDetailUiModel {
        return ProductDetailUiModel(
            id = product.id,
            name = product.name,
            articleText = product.articleNumber,
            accountingModelText = getAccountingModelText(product.accountingModel),
            units = mapProductUnits(product),
            barcodes = getAllBarcodes(product)
        )
    }

    private fun getAccountingModelText(model: AccountingModel): String {
        return when (model) {
            AccountingModel.BATCH -> resourceProvider.getString(R.string.accounting_model_batch)
            AccountingModel.QTY -> resourceProvider.getString(R.string.accounting_model_qty)
        }
    }

    private fun mapProductUnits(product: Product): List<ProductUnitUiModel> {
        return product.units.map { unit ->
            ProductUnitUiModel(
                id = unit.id,
                name = unit.name,
                quantityText = QuantityFormatter.format(unit.quantity),
                mainBarcode = unit.mainBarcode,
                isMainUnit = product.mainUnitId == unit.id,
                additionalBarcodesCount = unit.barcodes.size - 1 // Без основного штрихкода
            )
        }
    }

    private fun getAllBarcodes(product: Product): List<BarcodeUiModel> {
        val allBarcodes = mutableListOf<BarcodeUiModel>()

        // Собираем все штрихкоды из всех единиц измерения
        product.units.forEach { unit ->
            val isMainUnit = product.mainUnitId == unit.id

            // Основной штрихкод
            allBarcodes.add(BarcodeUiModel(
                barcode = unit.mainBarcode,
                isMainBarcode = isMainUnit
            ))

            // Дополнительные штрихкоды
            unit.barcodes.filter { it != unit.mainBarcode }.forEach { barcode ->
                allBarcodes.add(BarcodeUiModel(
                    barcode = barcode,
                    isMainBarcode = false
                ))
            }
        }

        return allBarcodes.distinctBy { it.barcode }
    }
}