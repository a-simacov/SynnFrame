package com.synngate.synnframe.domain.mapper

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicProductUnit

object DynamicProductMapper {

    fun toProduct(dynamicProduct: DynamicProduct): Product {
        return Product(
            id = dynamicProduct.id,
            name = dynamicProduct.name,
            accountingModel = dynamicProduct.getAccountingModelEnum(),
            articleNumber = dynamicProduct.articleNumber,
            mainUnitId = dynamicProduct.mainUnitId,
            units = dynamicProduct.units.map { toProductUnit(it) }
        )
    }

    private fun toProductUnit(dynamicUnit: DynamicProductUnit): ProductUnit {
        return ProductUnit(
            id = dynamicUnit.id,
            productId = dynamicUnit.productId,
            name = dynamicUnit.name,
            quantity = dynamicUnit.quantity,
            mainBarcode = dynamicUnit.mainBarcode,
            barcodes = dynamicUnit.barcodes
        )
    }

    fun toProductList(dynamicProducts: List<DynamicProduct>): List<Product> {
        return dynamicProducts.map { toProduct(it) }
    }
}