package com.synngate.synnframe.presentation.ui.products

import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductListItemUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductListState
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.resources.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

class ProductListViewModel(
    private val productUseCases: ProductUseCases,
    private val loggingService: LoggingService,
    private val soundService: SoundService,
    private val productUiMapper: ProductUiMapper,
    private val resourceProvider: ResourceProvider,
    private val isSelectionMode: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ProductListState, ProductListEvent>(
    ProductListState(isSelectionMode = isSelectionMode)
) {

    private var searchJob: Job? = null
    private val soundPlayedCache = ConcurrentHashMap<String, Long>()
    private val SOUND_DEBOUNCE_TIMEOUT = 1500L
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        observeProductsCount()
        loadProducts()
    }

    // Внутренний класс для UI-представления списка товаров
    data class ProductListUiPresentation(
        val products: List<ProductListItemUiModel>,
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
            products = currentState.products.map { product ->
                productUiMapper.mapToListItem(
                    product = product,
                    isSelected = product.id == currentState.selectedProduct?.id
                )
            },
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
                    sendEvent(ProductListEvent.ShowSnackbar("Товар со штрихкодом $barcode не найден"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")

                // Воспроизводим звук неуспешного сканирования с дебаунсингом
                playErrorSound(barcode)

                sendEvent(ProductListEvent.ShowSnackbar("Ошибка поиска по штрихкоду: ${e.message}"))
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

    fun findProductByBarcode(barcode: String, onProductFound: (Product?) -> Unit = {}) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    playSuccessSound(barcode)
                    launchMain { onProductFound(product) }
                } else {
                    playErrorSound(barcode)
                }

                updateState { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")

                // Воспроизводим звук неуспешного сканирования с дебаунсингом
                playErrorSound(barcode)

                updateState { it.copy(isLoading = false) }
                loggingService.logError("Ошибка поиска по штрихкоду: ${e.message}")
                // Вызываем колбэк с null в случае ошибки
                launchMain { onProductFound(null) }
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

    fun confirmProductSelection() {
        val selectedProduct = uiState.value.selectedProduct
        if (selectedProduct != null) {
            // Отправляем только ID продукта вместо целого объекта
            sendEvent(ProductListEvent.ReturnSelectedProductId(selectedProduct.id))
        }
    }
}