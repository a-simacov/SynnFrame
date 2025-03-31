package com.synngate.synnframe.data.repository

import androidx.room.withTransaction
import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.database.AppDatabase
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productApi: ProductApi,
    private val appDatabase: AppDatabase
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { productEntities ->
            buildProductsWithDetails(productEntities)
        }
    }

    override fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>> {
        return productDao.getProductsByNameFilter(nameFilter).map { productEntities ->
            buildProductsWithDetails(productEntities)
        }
    }

    override suspend fun getProductById(id: String): Product? {
        val productEntity = productDao.getProductById(id) ?: return null
        return buildProductWithDetails(productEntity)
    }

    override suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return withContext(Dispatchers.IO) {
            val productEntities = productDao.getProductEntitiesByIds(ids)
            buildProductsWithDetails(productEntities)
        }
    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        val productEntity = productDao.findProductByBarcode(barcode) ?: return null
        return buildProductWithDetails(productEntity)
    }

    override fun getProductsCount(): Flow<Int> {
        return productDao.getProductsCount()
    }

    override suspend fun addProduct(product: Product) {
        val productEntity = ProductEntity.fromDomainModel(product)
        productDao.insertProduct(productEntity)

        for (unit in product.units) {
            val unitEntity = ProductUnitEntity.fromDomainModel(unit)
            productDao.insertProductUnit(unitEntity)

            val barcodeEntities = BarcodeEntity.fromProductUnit(unit)
            for (barcodeEntity in barcodeEntities) {
                productDao.insertBarcode(barcodeEntity)
            }
        }
    }

    override suspend fun addProducts(products: List<Product>) {
        // Вся логика вставки товаров внутри транзакции
        val productEntities = products.map { ProductEntity.fromDomainModel(it) }
        val unitEntities = mutableListOf<ProductUnitEntity>()
        val barcodeEntities = mutableListOf<BarcodeEntity>()

        // Сбор всех связанных данных
        products.forEach { product ->
            product.units.forEach { unit ->
                unitEntities.add(ProductUnitEntity.fromDomainModel(unit))
                barcodeEntities.addAll(BarcodeEntity.fromProductUnit(unit))
            }
        }

        appDatabase.withTransaction {
            productDao.insertProducts(productEntities)
            productDao.insertProductUnits(unitEntities)
            productDao.insertBarcodes(barcodeEntities)
        }
    }

    override suspend fun updateProduct(product: Product) {
        val productEntity = ProductEntity.fromDomainModel(product)
        productDao.updateProduct(productEntity)

        productDao.deleteProductUnitsForProduct(product.id)
        productDao.deleteBarcodesForProduct(product.id)

        for (unit in product.units) {
            val unitEntity = ProductUnitEntity.fromDomainModel(unit)
            productDao.insertProductUnit(unitEntity)

            val barcodeEntities = BarcodeEntity.fromProductUnit(unit)
            for (barcodeEntity in barcodeEntities) {
                productDao.insertBarcode(barcodeEntity)
            }
        }
    }

    override suspend fun deleteProduct(id: String) {
        productDao.deleteProductById(id)
    }

    override suspend fun deleteAllProducts() {
        productDao.deleteAllBarcodes()
        productDao.deleteAllProductUnits()
        productDao.deleteAllProducts()
    }

    override suspend fun getProductsFromRemote(): ApiResult<List<Product>> {
        return productApi.getProducts()
    }

    private suspend fun buildProductWithDetails(productEntity: ProductEntity): Product {
        val unitEntities = productDao.getProductUnitsForProduct(productEntity.id)

        val units = unitEntities.map { unitEntity ->
            val barcodeEntities = productDao.getBarcodesForUnit(productEntity.id, unitEntity.id)
            val barcodes = barcodeEntities.map { it.code }

            unitEntity.toDomainModel(barcodes)
        }

        return productEntity.toDomainModel(units)
    }

    private suspend fun buildProductsWithDetails(productEntities: List<ProductEntity>): List<Product> {
        if (productEntities.isEmpty()) return emptyList()

        val productIds = productEntities.map { it.id }

        val allUnits = productDao.getProductUnitsForProducts(productIds)

        val unitIds = allUnits.map { it.id }

        val allBarcodes = productDao.getBarcodesForUnits(productIds, unitIds)

        val unitsMap = allUnits.groupBy { it.productId }
        val barcodesMap = allBarcodes.groupBy { "${it.productId}:${it.productUnitId}" }

        return productEntities.map { productEntity ->
            val units = unitsMap[productEntity.id]?.map { unitEntity ->
                val barcodes = barcodesMap["${productEntity.id}:${unitEntity.id}"]?.map { it.code }
                    ?: emptyList()

                unitEntity.toDomainModel(barcodes)
            } ?: emptyList()

            productEntity.toDomainModel(units)
        }
    }
}