package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

/**
 * Имплементация репозитория товаров
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productApi: ProductApi,
    private val logRepository: LogRepository
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

        logRepository.logInfo("Добавлен товар: ${product.name}")
    }

    override suspend fun addProducts(products: List<Product>) {
        for (product in products) {
            addProduct(product)
        }
        logRepository.logInfo("Добавлено товаров: ${products.size}")
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

        logRepository.logInfo("Обновлен товар: ${product.name}")
    }

    override suspend fun deleteProduct(id: String) {
        val product = productDao.getProductWithDetailsById(id)
        if (product != null) {
            // Удаляем товар (каскадно удалит единицы измерения и штрихкоды)
            productDao.deleteProductById(id)
            logRepository.logInfo("Удален товар: ${product.product.name}")
        }
    }

    override suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
        logRepository.logInfo("Удалены все товары")
    }

    override suspend fun syncProductsWithServer(): Result<Int> {
        return try {
            val response = productApi.getProducts()
            when (response) {
                is ApiResult.Success -> {
                    val products = response.data
                    if (products != null) {
                        // Очищаем существующие товары и добавляем новые
                        deleteAllProducts()
                        addProducts(products)

                        logRepository.logInfo("Синхронизировано товаров: ${products.size}")
                        Result.success(products.size)
                    } else {
                        logRepository.logWarning("Пустой ответ при синхронизации товаров")
                        Result.failure(IOException("Empty product synchronization response"))
                    }
                }

                is ApiResult.Error -> {
                    // Извлекаем сообщение об ошибке из ответа
                    val errorBody = response.message
                    logRepository.logWarning("Ошибка синхронизации товаров: $errorBody (${response.code})")
                    Result.failure(IOException("Product synchronization failed: $errorBody (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during product synchronization")
            logRepository.logError("Исключение при синхронизации товаров: ${e.message}")
            Result.failure(e)
        }
    }
}