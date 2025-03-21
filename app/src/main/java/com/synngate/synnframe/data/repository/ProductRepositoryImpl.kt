package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.ProductDao
import com.synngate.synnframe.data.local.entity.BarcodeEntity
import com.synngate.synnframe.data.local.entity.ProductEntity
import com.synngate.synnframe.data.local.entity.ProductUnitEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.ProductApi
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
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
        // Сначала ищем товар по штрихкоду
        val productEntity = productDao.findProductByBarcode(barcode) ?: return null
        return buildProductWithDetails(productEntity)
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
        // Удаляем все записи в правильном порядке с учетом зависимостей
        productDao.deleteAllBarcodes()
        productDao.deleteAllProductUnits()
        productDao.deleteAllProducts()
    }

    override suspend fun getProductsFromRemote(): ApiResult<List<Product>> {
        return productApi.getProducts()
    }

    /**
     * Построение доменной модели Product из ProductEntity и связанных данных
     */
    private suspend fun buildProductWithDetails(productEntity: ProductEntity): Product {
        // Получаем единицы измерения для товара
        val unitEntities = productDao.getProductUnitsForProduct(productEntity.id)

        // Строим доменные модели единиц измерения с их штрихкодами
        val units = unitEntities.map { unitEntity ->
            val barcodeEntities = productDao.getBarcodesForUnit(unitEntity.id)
            val barcodes = barcodeEntities.map { it.code }

            unitEntity.toDomainModel(barcodes)
        }

        // Возвращаем полную доменную модель товара
        return productEntity.toDomainModel(units)
    }

    /**
     * Построение списка доменных моделей Product из списка ProductEntity и связанных данных
     */
    private suspend fun buildProductsWithDetails(productEntities: List<ProductEntity>): List<Product> {
        if (productEntities.isEmpty()) return emptyList()

        // Получаем ID всех товаров
        val productIds = productEntities.map { it.id }

        // Загружаем все единицы измерения за один запрос
        val allUnits = productDao.getProductUnitsForProducts(productIds)

        // Получаем ID всех единиц измерения
        val unitIds = allUnits.map { it.id }

        // Загружаем все штрихкоды за один запрос
        val allBarcodes = productDao.getBarcodesForUnits(unitIds)

        // Группируем единицы измерения и штрихкоды для быстрого доступа
        val unitsMap = allUnits.groupBy { it.productId }
        val barcodesMap = allBarcodes.groupBy { it.productUnitId }

        // Собираем доменные модели товаров
        return productEntities.map { productEntity ->
            // Получаем единицы измерения для текущего товара
            val units = unitsMap[productEntity.id]?.map { unitEntity ->
                // Получаем штрихкоды для текущей единицы измерения
                val barcodes = barcodesMap[unitEntity.id]?.map { it.code } ?: emptyList()

                // Создаем доменную модель единицы измерения
                unitEntity.toDomainModel(barcodes)
            } ?: emptyList()

            // Создаем доменную модель товара
            productEntity.toDomainModel(units)
        }
    }
}