package com.synngate.synnframe.presentation.ui.products

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.products.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductListState
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

class ProductListViewModel(
    private val productUseCases: ProductUseCases,
    private val isSelectionMode: Boolean = false,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ProductListState, ProductListEvent>(
    ProductListState(isSelectionMode = isSelectionMode)
) {

    private var searchJob: Job? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        observeProductsCount()
        loadProducts()
    }

    private fun observeProductsCount() {
        launchIO {
            productUseCases.getProductsCount().collectLatest { count ->
                updateState { it.copy(productsCount = count) }
            }
        }
    }

    private fun loadProducts() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                productUseCases.getProducts().collectLatest { products ->
                    // Применяем сортировку и фильтрацию к полученным данным
                    val processedProducts = processProducts(products)

                    updateState { state ->
                        state.copy(
                            products = processedProducts,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading products")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                sendEvent(ProductListEvent.ShowSnackbar("Ошибка загрузки товаров: ${e.message}"))
            }
        }
    }

    private fun processProducts(products: List<Product>): List<Product> {
        val state = uiState.value

        // Фильтрация по модели учета, если задана
        var filteredProducts = if (state.filterByAccountingModel != null) {
            products.filter { it.accountingModel == state.filterByAccountingModel }
        } else {
            products
        }

        // Применяем сортировку в соответствии с выбранным порядком
        val sortedProducts = when (state.sortOrder) {
            SortOrder.NAME_ASC -> filteredProducts.sortedBy { it.name }
            SortOrder.NAME_DESC -> filteredProducts.sortedByDescending { it.name }
            SortOrder.ARTICLE_ASC -> filteredProducts.sortedBy { it.articleNumber }
            SortOrder.ARTICLE_DESC -> filteredProducts.sortedByDescending { it.articleNumber }
        }

        return sortedProducts
    }

    @OptIn(FlowPreview::class)
    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }

        // Отменяем предыдущий поиск, если он выполняется
        searchJob?.cancel()

        searchJob = launchIO {
            try {
                if (query.isEmpty()) {
                    loadProducts()
                } else {
                    updateState { it.copy(isLoading = true) }

                    // Используем debounce для предотвращения частых запросов при быстром вводе
                    productUseCases.getProductsByNameFilter(query)
                        .debounce(300)
                        .collectLatest { products ->
                            // Применяем дополнительные фильтры и сортировку
                            val processedProducts = processProducts(products)

                            updateState { state ->
                                state.copy(
                                    products = processedProducts,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                }
            } catch (e: CancellationException) {
                // Игнорируем исключения отмены и сбрасываем статус загрузки
                updateState { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error searching products")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
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

    fun updateSortOrder(sortOrder: SortOrder) {
        updateState { it.copy(sortOrder = sortOrder) }
        loadProducts()
    }

    fun syncProducts() {
        launchIO {
            updateState { it.copy(isSyncing = true) }

            try {
                loggingService.logInfo("Начата синхронизация товаров")

                val result = productUseCases.syncProductsWithServer()

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    val formattedTime = LocalDateTime.now().format(dateFormatter)

                    updateState { it.copy(
                        isSyncing = false,
                        lastSyncTime = formattedTime
                    ) }

                    loggingService.logInfo("Синхронизация товаров завершена успешно. Обновлено $count товаров.")
                    sendEvent(ProductListEvent.ShowSnackbar("Синхронизация выполнена. Обновлено $count товаров."))

                    // Перезагружаем список товаров после синхронизации
                    loadProducts()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"

                    updateState { it.copy(isSyncing = false) }
                    loggingService.logError("Ошибка синхронизации товаров: $error")
                    sendEvent(ProductListEvent.ShowSnackbar("Ошибка синхронизации: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing products")

                updateState { it.copy(isSyncing = false) }
                loggingService.logError("Исключение при синхронизации товаров: ${e.message}")
                sendEvent(ProductListEvent.ShowSnackbar("Ошибка синхронизации: ${e.message}"))
            }
        }
    }

    fun navigateBack() {
        sendEvent(ProductListEvent.NavigateBack)
    }

    fun findProductByBarcode(barcode: String) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    // Если товар найден и находимся в режиме выбора, сразу возвращаем его
                    if (uiState.value.isSelectionMode) {
                        sendEvent(ProductListEvent.ReturnToTaskWithProduct(product))
                    } else {
                        // Иначе открываем подробную информацию
                        sendEvent(ProductListEvent.NavigateToProductDetail(product.id))
                    }
                } else {
                    // Если товар не найден, показываем сообщение
                    sendEvent(ProductListEvent.ShowSnackbar("Товар со штрихкодом $barcode не найден"))
                }

                updateState { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")
                updateState { it.copy(isLoading = false) }
                sendEvent(ProductListEvent.ShowSnackbar("Ошибка поиска по штрихкоду: ${e.message}"))
            }
        }
    }

    fun startBatchScanning() {
        updateState { it.copy(showBatchScannerDialog = true) }
    }

    fun finishBatchScanning() {
        updateState { it.copy(showBatchScannerDialog = false) }
    }

    fun processBatchScanResults(results: List<ScanResult>) {
        // Логируем результаты сканирования
        launchIO {
            val foundCount = results.count { it.product != null }
            val notFoundCount = results.size - foundCount

            loggingService.logInfo(
                "Завершено пакетное сканирование: найдено $foundCount товаров, не найдено $notFoundCount"
            )

            // Показываем сообщение с результатами
            sendEvent(
                ProductListEvent.ShowSnackbar(
                    "Сканирование завершено: найдено $foundCount, не найдено $notFoundCount"
                )
            )
        }

        // Закрываем диалог сканирования
        finishBatchScanning()
    }

    fun startScanning() {
        updateState { it.copy(showScannerDialog = true) }
    }

    fun finishScanning() {
        updateState { it.copy(showScannerDialog = false) }
    }

    /**
     * Подтверждает выбор товара и возвращается к заданию
     */
    fun confirmProductSelection() {
        val selectedProduct = uiState.value.selectedProduct
        if (selectedProduct != null) {
            sendEvent(ProductListEvent.ReturnToTaskWithProduct(selectedProduct))
        }
    }
}