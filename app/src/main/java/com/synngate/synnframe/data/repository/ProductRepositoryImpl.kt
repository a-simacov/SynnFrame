package com.synngate.synnframe.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val productApi: ProductApi,
    private val appDatabase: AppDatabase
) : ProductRepository {
    // Размер страницы для пагинации
    private val PAGE_SIZE = 20

    // Константа для максимального размера батча в SQL запросах
    private val MAX_BATCH_SIZE = 400

    // Реализация методов с поддержкой пагинации и сортировки
    override fun getProductsPaged(sortOrder: SortOrder): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 3 * PAGE_SIZE
            ),
            pagingSourceFactory = { 
                when (sortOrder) {
                    SortOrder.NAME_ASC -> productDao.getProductsPagedByNameAsc()
                    SortOrder.NAME_DESC -> productDao.getProductsPagedByNameDesc()
                    SortOrder.ARTICLE_ASC -> productDao.getProductsPagedByArticleAsc()
                    SortOrder.ARTICLE_DESC -> productDao.getProductsPagedByArticleDesc()
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { productEntity ->
                buildProductWithDetails(productEntity)
            }
        }
    }

    override fun getProductsByNameFilterPaged(nameFilter: String, sortOrder: SortOrder): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 3 * PAGE_SIZE
            ),
            pagingSourceFactory = { 
                when (sortOrder) {
                    SortOrder.NAME_ASC -> productDao.getProductsByNameFilterPagedByNameAsc(nameFilter)
                    SortOrder.NAME_DESC -> productDao.getProductsByNameFilterPagedByNameDesc(nameFilter)
                    SortOrder.ARTICLE_ASC -> productDao.getProductsByNameFilterPagedByArticleAsc(nameFilter)
                    SortOrder.ARTICLE_DESC -> productDao.getProductsByNameFilterPagedByArticleDesc(nameFilter)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { productEntity ->
                buildProductWithDetails(productEntity)
            }
        }
    }

    // Существующие методы с оптимизацией
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
        // Оптимизируем, используя новый метод для получения продукта со связями в одном запросе
        val productWithRelations = productDao.getProductWithRelationsById(id)
        return productWithRelations?.toDomainModel()
    }

    override suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext emptyList()

            // Разбиваем множество ID на батчи для избежания ошибки "too many SQL variables"
            val resultProducts = mutableListOf<Product>()

            // Используем меньший размер батча для безопасности
            val idBatches = ids.chunked(MAX_BATCH_SIZE)

            for (idBatch in idBatches) {
                val productEntities = productDao.getProductEntitiesByIds(idBatch.toSet())
                val batchProducts = buildProductsWithDetailsOptimized(productEntities)
                resultProducts.addAll(batchProducts)
            }

            resultProducts
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

        // Разбиваем списки на батчи, чтобы избежать ошибки "too many SQL variables"
        appDatabase.withTransaction {
            // Вставляем продукты батчами
            productEntities.chunked(MAX_BATCH_SIZE).forEach { batch ->
                productDao.insertProducts(batch)
            }

            // Вставляем единицы измерения батчами
            unitEntities.chunked(MAX_BATCH_SIZE).forEach { batch ->
                productDao.insertProductUnits(batch)
            }

            // Вставляем штрихкоды батчами
            barcodeEntities.chunked(MAX_BATCH_SIZE).forEach { batch ->
                productDao.insertBarcodes(batch)
            }
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

    // Оптимизированный метод для загрузки деталей продукта
    private suspend fun buildProductWithDetails(productEntity: ProductEntity): Product {
        // Получаем единицы измерения для продукта
        val unitEntities = productDao.getProductUnitsForProduct(productEntity.id)

        val units = unitEntities.map { unitEntity ->
            // Получаем штрихкоды для единицы измерения
            val barcodeEntities = productDao.getBarcodesForUnit(productEntity.id, unitEntity.id)
            val barcodes = barcodeEntities.map { it.code }

            unitEntity.toDomainModel(barcodes)
        }

        return productEntity.toDomainModel(units)
    }

    // Оптимизированный метод для загрузки деталей для списка продуктов
    private suspend fun buildProductsWithDetailsOptimized(productEntities: List<ProductEntity>): List<Product> {
        if (productEntities.isEmpty()) return emptyList()

        val resultProducts = mutableListOf<Product>()
        val productIds = productEntities.map { it.id }

        // Загружаем все единицы измерения для всех продуктов одним запросом
        val allUnits = productDao.getProductUnitsForProductsBatch(productIds)

        // Загружаем все штрихкоды для всех продуктов одним запросом
        val allBarcodes = productDao.getBarcodesForProductsBatch(productIds)

        // Группируем единицы измерения по productId
        val unitsMap = allUnits.groupBy { it.productId }

        // Группируем штрихкоды по productId и unitId
        val barcodesMap = allBarcodes.groupBy { it.productId }
            .mapValues { (_, barcodes) -> barcodes.groupBy { it.productUnitId } }

        // Собираем продукты с их единицами измерения и штрихкодами
        for (productEntity in productEntities) {
            val productUnits = unitsMap[productEntity.id]?.map { unitEntity ->
                val unitBarcodes = barcodesMap[productEntity.id]?.get(unitEntity.id)?.map { it.code } ?: emptyList()
                unitEntity.toDomainModel(unitBarcodes)
            } ?: emptyList()

            resultProducts.add(productEntity.toDomainModel(productUnits))
        }

        return resultProducts
    }

    // Оригинальный метод с оптимизацией батчей
    private suspend fun buildProductsWithDetails(productEntities: List<ProductEntity>): List<Product> {
        if (productEntities.isEmpty()) return emptyList()

        // Максимальный безопасный размер батча (меньше лимита SQLite)
        val BATCH_SIZE = MAX_BATCH_SIZE

        val resultProducts = mutableListOf<Product>()

        // Разбиваем список товаров на батчи
        val productBatches = productEntities.chunked(BATCH_SIZE)

        for (batch in productBatches) {
            val productIds = batch.map { it.id }

            val allUnits = productDao.getProductUnitsForProducts(productIds)
            val unitIds = allUnits.map { it.id }

            // Разбиваем запрос штрихкодов на батчи, чтобы избежать ошибки "too many SQL variables"
            val allBarcodes = mutableListOf<BarcodeEntity>()
            for (productIdBatch in productIds.chunked(BATCH_SIZE / 2)) {
                for (unitIdBatch in unitIds.chunked(BATCH_SIZE / 2)) {
                    val barcodes = productDao.getBarcodesForUnits(productIdBatch, unitIdBatch)
                    allBarcodes.addAll(barcodes)
                }
            }

            val unitsMap = allUnits.groupBy { it.productId }
            val barcodesMap = allBarcodes.groupBy { "${it.productId}:${it.productUnitId}" }

            val batchProducts = batch.map { productEntity ->
                val units = unitsMap[productEntity.id]?.map { unitEntity ->
                    val barcodes = barcodesMap["${productEntity.id}:${unitEntity.id}"]?.map { it.code }
                        ?: emptyList()
                    unitEntity.toDomainModel(barcodes)
                } ?: emptyList()

                productEntity.toDomainModel(units)
            }

            resultProducts.addAll(batchProducts)
        }

        return resultProducts
    }
}