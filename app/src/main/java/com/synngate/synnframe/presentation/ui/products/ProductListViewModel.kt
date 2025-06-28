package com.synngate.synnframe.presentation.ui.products

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductListItemUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductListState
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.resources.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class ProductListViewModel(
    private val productUseCases: ProductUseCases,
    private val soundService: SoundService,
    private val synchronizationController: SynchronizationController,
    private val productUiMapper: ProductUiMapper,
    private val resourceProvider: ResourceProvider,
    private val isSelectionMode: Boolean = false,
) : BaseViewModel<ProductListState, ProductListEvent>(
    ProductListState(isSelectionMode = isSelectionMode)
) {
    // Состояние поискового запроса
    private val _searchQuery = MutableStateFlow("")

    // Поток пагинированных данных продуктов
    private val _productsFlow: Flow<PagingData<Product>> = createPagingDataFlow()

    // Поток пагинированных UI-моделей для отображения в списке
    val productItemsFlow: Flow<PagingData<ProductListItemUiModel>> = _productsFlow
        .map { pagingData ->
            pagingData.map { product ->
                productUiMapper.mapToListItem(
                    product = product,
                    isSelected = product.id == uiState.value.selectedProduct?.id
                )
            }
        }
        .cachedIn(viewModelScope)

    private var searchJob: Job? = null
    private val soundPlayedCache = ConcurrentHashMap<String, Long>()
    private val SOUND_DEBOUNCE_TIMEOUT = 1500L

    init {
        observeProductsCount()
    }

    // Внутренний класс для UI-представления списка товаров
    data class ProductListUiPresentation(
        val totalCount: Int,
        val filteredCount: Int,
        val isLoading: Boolean,
        val errorMessage: String?,
        val searchQuery: String,
        val sortLabel: String,
        val isSelectionMode: Boolean,
        val selectedProductId: String?,
        val showConfirmButton: Boolean
    )

    // Метод для получения UI-состояния для отображения
    fun getUiPresentation(): ProductListUiPresentation {
        val currentState = uiState.value

        return ProductListUiPresentation(
            totalCount = currentState.productsCount,
            filteredCount = currentState.products.size,
            isLoading = currentState.isLoading,
            errorMessage = currentState.error,
            searchQuery = currentState.searchQuery,
            sortLabel = getSortOrderLabel(currentState.sortOrder),
            isSelectionMode = currentState.isSelectionMode,
            selectedProductId = currentState.selectedProduct?.id,
            showConfirmButton = currentState.isSelectionMode && currentState.selectedProduct != null
        )
    }

    private fun getSortOrderLabel(sortOrder: SortOrder): String {
        return when (sortOrder) {
            SortOrder.NAME_ASC -> resourceProvider.getString(R.string.sort_by_name_asc)
            SortOrder.NAME_DESC -> resourceProvider.getString(R.string.sort_by_name_desc)
            SortOrder.ARTICLE_ASC -> resourceProvider.getString(R.string.sort_by_article_asc)
            SortOrder.ARTICLE_DESC -> resourceProvider.getString(R.string.sort_by_article_desc)
        }
    }

    private fun observeProductsCount() {
        launchIO {
            productUseCases.getProductsCount().collectLatest { count ->
                updateState { it.copy(productsCount = count) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createPagingDataFlow(): Flow<PagingData<Product>> {
        return _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isEmpty()) {
                    productUseCases.getProductsPaged()
                } else {
                    productUseCases.getProductsByNameFilterPaged(query)
                }
            }
            .cachedIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun onProductClick(product: Product) {
        if (uiState.value.isSelectionMode) {
            // В режиме выбора товара выделяем товар и показываем кнопку подтверждения
            updateState { it.copy(selectedProduct = product) }
        } else {
            // В обычном режиме переходим к деталям товара
            sendEvent(ProductListEvent.NavigateToProductDetail(product.id))
        }
    }

    fun onProductItemClick(productId: String) {
        if (uiState.value.isSelectionMode) {
            // В режиме выбора нужно найти товар для выделения
            launchIO {
                val product = productUseCases.getProductById(productId)
                if (product != null) {
                    updateState { it.copy(selectedProduct = product) }
                }
            }
        } else {
            // В обычном режиме переходим к деталям товара
            sendEvent(ProductListEvent.NavigateToProductDetail(productId))
        }
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        updateState { it.copy(sortOrder = sortOrder) }
        // Примечание: в текущей реализации пагинации мы не можем изменить сортировку на лету
        // Здесь нужно будет изменить логику запросов к базе данных или создать новый PagingSource
    }

    fun syncProducts() {
        launchIO {
            updateState { it.copy(isSyncing = true) }

            try {
                // Используем контроллер для синхронизации только товаров
                val result = productUseCases.syncProductsWithServer()

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0

                    // Обновляем информацию о синхронизации в контроллере
                    synchronizationController.updateLastProductsSync(count)
                    val lastSyncTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                    updateState { it.copy(isSyncing = false, lastSyncTime = lastSyncTime) }
                    sendEvent(ProductListEvent.ShowSnackbar(
                        "Product synchronization completed. Updated: $count"
                    ))
                } else {
                    updateState { it.copy(isSyncing = false) }
                    sendEvent(ProductListEvent.ShowSnackbar(
                        "Synchronization error: ${result.exceptionOrNull()?.message}"
                    ))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing products")
                updateState { it.copy(isLoading = false) }
                sendEvent(ProductListEvent.ShowSnackbar(
                    "Synchronization error: ${e.message}"
                ))
            }
        }
    }

    fun handleScannedBarcode(barcode: String) {
        launchIO {
            updateState { it.copy(isLoading = true, showScannerDialog = false) }

            try {
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    // Воспроизводим звук успешного сканирования с дебаунсингом
                    playSuccessSound(barcode)

                    // Если товар найден, посылаем событие навигации
                    if (uiState.value.isSelectionMode) {
                        sendEvent(ProductListEvent.ReturnSelectedProductId(product.id))
                    } else {
                        sendEvent(ProductListEvent.NavigateToProductDetail(product.id))
                    }
                } else {
                    // Воспроизводим звук неуспешного сканирования с дебаунсингом
                    playErrorSound(barcode)

                    // Если товар не найден, показываем сообщение
                    sendEvent(ProductListEvent.ShowSnackbar("Product with barcode $barcode not found"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")

                // Воспроизводим звук неуспешного сканирования с дебаунсингом
                playErrorSound(barcode)

                sendEvent(ProductListEvent.ShowSnackbar("Barcode search error: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun navigateBack() {
        sendEvent(ProductListEvent.NavigateBack)
    }

    fun navigateToProductDetail(productId: String) {
        sendEvent(ProductListEvent.NavigateToProductDetail(productId))
    }

    fun findProductByBarcode(barcode: String, onResult: (Product?) -> Unit) {
        launchIO {
            try {
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    playSuccessSound(barcode)
                    launchMain {
                        onResult(product)
                    }
                } else {
                    playErrorSound(barcode)
                    sendEvent(ProductListEvent.ShowSnackbar("Product with barcode $barcode not found"))
                    launchMain {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")
                playErrorSound(barcode)
                sendEvent(ProductListEvent.ShowSnackbar("Error finding product: ${e.message}"))
                launchMain {
                    onResult(null)
                }
            }
        }
    }

    // Метод для воспроизведения звука успешного сканирования с дебаунсингом
    private fun playSuccessSound(barcode: String) {
        playSound(barcode, true)
    }

    // Метод для воспроизведения звука неуспешного сканирования с дебаунсингом
    private fun playErrorSound(barcode: String) {
        playSound(barcode, false)
    }

    // Обновленный метод для воспроизведения звука с дебаунсингом
    private fun playSound(barcode: String, success: Boolean) {
        val currentTime = System.currentTimeMillis()
        val lastSoundTime = soundPlayedCache[barcode]

        // Проверяем, был ли звук недавно воспроизведен для этого штрихкода
        if (lastSoundTime == null || (currentTime - lastSoundTime > SOUND_DEBOUNCE_TIMEOUT)) {
            // Обновляем время последнего воспроизведения звука
            soundPlayedCache[barcode] = currentTime

            // Воспроизводим звук
            if (success) {
                soundService.playSuccessSound()
            } else {
                soundService.playErrorSound()
            }

            // Активно очищаем кэш для всех штрихкодов, у которых прошло более
            // SOUND_DEBOUNCE_TIMEOUT мс после последнего воспроизведения
            cleanExpiredSoundCache(currentTime)
        } else {
            Timber.d("Sound for barcode $barcode debounced. Last sound was ${currentTime - lastSoundTime}ms ago")
        }
    }

    // Новый метод для очистки устаревших записей в кэше звуков
    private fun cleanExpiredSoundCache(currentTime: Long) {
        soundPlayedCache.entries.removeIf { (_, timestamp) ->
            currentTime - timestamp > SOUND_DEBOUNCE_TIMEOUT
        }
    }

    fun startBatchScanning() {
        updateState { it.copy(showBatchScannerDialog = true) }
    }

    fun finishBatchScanning() {
        updateState { it.copy(showBatchScannerDialog = false) }
    }

    fun processBatchScanResults(results: List<ScanResult>) {
        launchIO {
            val foundCount = results.count { it.product != null }
            val notFoundCount = results.size - foundCount

            Timber.i("Batch scanning finished: found $foundCount products, not found $notFoundCount")

            sendEvent(
                ProductListEvent.ShowSnackbar(
                    "Scanning completed: found $foundCount, not found $notFoundCount"
                )
            )
        }

        finishBatchScanning()
    }

    fun startScanning() {
        updateState { it.copy(showScannerDialog = true) }
    }

    fun finishScanning() {
        updateState { it.copy(showScannerDialog = false) }
    }

    fun confirmProductSelection() {
        val selectedProduct = uiState.value.selectedProduct
        if (selectedProduct != null) {
            sendEvent(ProductListEvent.ReturnSelectedProductId(selectedProduct.id))
        }
    }

}
