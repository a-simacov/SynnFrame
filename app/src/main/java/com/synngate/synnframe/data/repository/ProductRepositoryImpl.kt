package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.ProductDao
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

/**
 * Имплементация репозитория товаров
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productApi: ProductApi
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProductsWithDetails().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>> {
        return productDao.getProductsByNameFilter(nameFilter).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductWithDetailsById(id)?.toDomainModel()
    }

    override suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return withContext(Dispatchers.IO) {
            // Получаем список продуктов по одному, используя существующий метод
            ids.mapNotNull { productId -> getProductById(productId) }
        }

    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        return productDao.findProductByBarcode(barcode)?.toDomainModel()
    }

    override fun getProductsCount(): Flow<Int> {
        return productDao.getProductsCount()
    }

    override suspend fun addProduct(product: Product) {
        // Вставляем основную информацию о товаре
        val productEntity = ProductEntity.fromDomainModel(product)
        productDao.insertProduct(productEntity)

        // Вставляем единицы измерения товара
        for (unit in product.units) {
            val unitEntity = ProductUnitEntity.fromDomainModel(unit)
            productDao.insertProductUnit(unitEntity)

            // Вставляем штрихкоды для каждой единицы измерения
            val barcodeEntities = BarcodeEntity.fromProductUnit(unit)
            for (barcodeEntity in barcodeEntities) {
                productDao.insertBarcode(barcodeEntity)
            }
        }
    }

    override suspend fun addProducts(products: List<Product>) {
        for (product in products) {
            addProduct(product)
        }
    }

    override suspend fun updateProduct(product: Product) {
        // Обновляем основную информацию о товаре
        val productEntity = ProductEntity.fromDomainModel(product)
        productDao.updateProduct(productEntity)

        // Удаляем все единицы измерения и штрихкоды товара
        productDao.deleteProductUnitsForProduct(product.id)
        productDao.deleteBarcodesForProduct(product.id)

        // Вставляем обновленные единицы измерения и штрихкоды
        for (unit in product.units) {
            val unitEntity = ProductUnitEntity.fromDomainModel(unit)
            productDao.insertProductUnit(unitEntity)

            // Вставляем штрихкоды для каждой единицы измерения
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
        productDao.deleteAllProducts()
    }

    override suspend fun getProductsFromRemote(): ApiResult<List<Product>> {
        return productApi.getProducts()
    }
}