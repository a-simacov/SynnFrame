package com.synngate.synnframe.domain.repository
import androidx.paging.PagingData
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    // Существующие методы
    fun getProducts(): Flow<List<Product>>

    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>>

    // Новые методы с поддержкой пагинации и сортировки
    fun getProductsPaged(sortOrder: SortOrder = SortOrder.NAME_ASC): Flow<PagingData<Product>>

    fun getProductsByNameFilterPaged(nameFilter: String, sortOrder: SortOrder = SortOrder.NAME_ASC): Flow<PagingData<Product>>

    suspend fun getProductById(id: String): Product?

    suspend fun getProductsByIds(ids: Set<String>): List<Product>

    suspend fun findProductByBarcode(barcode: String): Product?

    fun getProductsCount(): Flow<Int>

    suspend fun addProduct(product: Product)

    suspend fun addProducts(products: List<Product>)

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(id: String)

    suspend fun deleteAllProducts()

    suspend fun getProductsFromRemote(): ApiResult<List<Product>>
}
