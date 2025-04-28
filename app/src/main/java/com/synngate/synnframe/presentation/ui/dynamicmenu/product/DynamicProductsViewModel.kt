package com.synngate.synnframe.presentation.ui.dynamicmenu.product

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.mapper.DynamicProductMapper
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductsEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductsState
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.ProductListItemUiModel
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

class DynamicProductsViewModel(
    val menuItemId: String,
    val menuItemName: String,
    val endpoint: String,
    val screenSettings: ScreenSettings,
    private val dynamicMenuUseCases: DynamicMenuUseCases,
    private val soundService: SoundService,
    private val productUiMapper: ProductUiMapper,
    private val isSelectionMode: Boolean = false
) : BaseViewModel<DynamicProductsState, DynamicProductsEvent>(
    DynamicProductsState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        endpoint = endpoint,
        screenSettings = screenSettings,
        isSelectionMode = isSelectionMode
    )
) {
    private var searchJob: Job? = null
    private val soundPlayedCache = ConcurrentHashMap<String, Long>()
    private val SOUND_DEBOUNCE_TIMEOUT = 1500L

    init {
        if (uiState.value.hasElement(ScreenElementType.SHOW_LIST)) {
            loadDynamicProducts()
        }
    }

    // Внутренний класс для UI-представления списка товаров
    data class ProductListUiPresentation(
        val products: List<ProductListItemUiModel>,
        val totalCount: Int,
        val filteredCount: Int,
        val isLoading: Boolean,
        val errorMessage: String?,
        val searchQuery: String,
        val isSelectionMode: Boolean,
        val selectedProductId: String?,
        val showConfirmButton: Boolean
    )

    // Метод для получения UI-состояния для отображения
    fun getUiPresentation(): ProductListUiPresentation {
        val currentState = uiState.value

        // Преобразуем динамические товары в обычные для использования с существующим UI-маппером
        val mappedProducts = currentState.products.map { DynamicProductMapper.toProduct(it) }

        return ProductListUiPresentation(
            products = mappedProducts.map { product ->
                productUiMapper.mapToListItem(
                    product = product,
                    isSelected = product.id == currentState.selectedProduct?.id
                )
            },
            totalCount = currentState.products.size,
            filteredCount = currentState.products.size,
            isLoading = currentState.isLoading,
            errorMessage = currentState.error,
            searchQuery = currentState.searchValue,
            isSelectionMode = currentState.isSelectionMode,
            selectedProductId = currentState.selectedProduct?.id,
            showConfirmButton = currentState.isSelectionMode && currentState.selectedProduct != null
        )
    }

    fun loadDynamicProducts() {
        val currentEndpoint = uiState.value.endpoint
        if (currentEndpoint.isEmpty()) {
            updateState { it.copy(error = "Ошибка: не указан endpoint для загрузки товаров") }
            return
        }

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getDynamicProducts(currentEndpoint)
                if (result.isSuccess()) {
                    val products = result.getOrNull() ?: emptyList()

                    // Если получен только один товар и настройки указывают автоматически открывать детали
                    if (products.size == 1 && uiState.value.screenSettings.openImmediately) {
                        sendEvent(DynamicProductsEvent.NavigateToProductDetail(products[0]))
                    }

                    updateState {
                        it.copy(
                            products = products,
                            isLoading = false,
                            error = if (products.isEmpty()) "Нет доступных товаров" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки товаров: ${(result as? ApiResult.Error)?.message}"
                    Timber.e(errorMessage)
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading dynamic products")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки товаров: ${e.message}"
                    )
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchValue = query) }

        // Отменяем предыдущий поиск, если он выполняется
        searchJob?.cancel()

        searchJob = launchIO {
            try {
                if (query.isEmpty()) {
                    loadDynamicProducts()
                } else {
                    updateState { it.copy(isLoading = true) }

                    // Используем debounce для предотвращения частых запросов при быстром вводе
                    val params = mapOf("query" to query)
                    val result = dynamicMenuUseCases.getDynamicProducts(
                        endpoint = uiState.value.endpoint,
                        params = params
                    )

                    if (result.isSuccess()) {
                        val products = result.getOrNull() ?: emptyList()
                        updateState { state ->
                            state.copy(
                                products = products,
                                isLoading = false,
                                error = if (products.isEmpty()) "Товары не найдены" else null
                            )
                        }
                    } else {
                        updateState { state ->
                            state.copy(
                                isLoading = false,
                                error = "Ошибка поиска: ${(result as? ApiResult.Error)?.message}"
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

    fun onProductClick(product: DynamicProduct) {
        if (uiState.value.isSelectionMode) {
            // В режиме выбора товара выделяем товар и показываем кнопку подтверждения
            updateState { it.copy(selectedProduct = product) }
        } else {
            // В обычном режиме переходим к деталям товара
            sendEvent(DynamicProductsEvent.NavigateToProductDetail(product))
        }
    }

    fun onSearch() {
        val searchValue = uiState.value.searchValue.trim()
        if (searchValue.isEmpty()) {
            sendEvent(DynamicProductsEvent.ShowSnackbar("Введите значение для поиска"))
            return
        }

        // Поиск с использованием значения из поля поиска
        val params = mapOf("query" to searchValue)
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val result = dynamicMenuUseCases.getDynamicProducts(
                    endpoint = uiState.value.endpoint,
                    params = params
                )

                if (result.isSuccess()) {
                    val products = result.getOrNull() ?: emptyList()
                    updateState {
                        it.copy(
                            products = products,
                            isLoading = false,
                            error = if (products.isEmpty()) "Товары не найдены" else null
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка поиска: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun handleScannedBarcode(barcode: String) {
        launchIO {
            updateState { it.copy(isLoading = true, showScannerDialog = false) }

            try {
                // Поиск товара по штрихкоду
                val params = mapOf("barcode" to barcode)
                val result = dynamicMenuUseCases.getDynamicProducts(
                    endpoint = uiState.value.endpoint,
                    params = params
                )

                if (result.isSuccess()) {
                    val products = result.getOrNull() ?: emptyList()
                    if (products.isNotEmpty()) {
                        // Воспроизводим звук успешного сканирования с дебаунсингом
                        playSuccessSound(barcode)

                        // Если товар найден
                        val product = products.first()
                        if (uiState.value.isSelectionMode) {
                            sendEvent(DynamicProductsEvent.ReturnSelectedProductId(product.id))
                        } else {
                            sendEvent(DynamicProductsEvent.NavigateToProductDetail(product))
                        }
                    } else {
                        // Воспроизводим звук неуспешного сканирования с дебаунсингом
                        playErrorSound(barcode)

                        // Если товар не найден, показываем сообщение
                        sendEvent(DynamicProductsEvent.ShowSnackbar("Товар со штрихкодом $barcode не найден"))
                    }
                } else {
                    // Воспроизводим звук неуспешного сканирования с дебаунсингом
                    playErrorSound(barcode)

                    sendEvent(DynamicProductsEvent.ShowSnackbar("Ошибка поиска товара: ${(result as? ApiResult.Error)?.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")

                // Воспроизводим звук неуспешного сканирования с дебаунсингом
                playErrorSound(barcode)

                sendEvent(DynamicProductsEvent.ShowSnackbar("Ошибка поиска по штрихкоду: ${e.message}"))
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun findProductByBarcode(barcode: String, onProductFound: (DynamicProduct?) -> Unit) {
        launchIO {
            try {
                // Поиск товара по штрихкоду
                val params = mapOf("barcode" to barcode)
                val result = dynamicMenuUseCases.getDynamicProducts(
                    endpoint = uiState.value.endpoint,
                    params = params
                )

                if (result.isSuccess()) {
                    val products = result.getOrNull() ?: emptyList()
                    if (products.isNotEmpty()) {
                        playSuccessSound(barcode)
                        launchMain {
                            onProductFound(products.first())
                        }
                    } else {
                        playErrorSound(barcode)
                        sendEvent(DynamicProductsEvent.ShowSnackbar("Товар со штрихкодом $barcode не найден"))
                        launchMain {
                            onProductFound(null)
                        }
                    }
                } else {
                    playErrorSound(barcode)
                    sendEvent(DynamicProductsEvent.ShowSnackbar("Ошибка поиска товара: ${(result as? ApiResult.Error)?.message}"))
                    launchMain {
                        onProductFound(null)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finding product by barcode")
                playErrorSound(barcode)
                sendEvent(DynamicProductsEvent.ShowSnackbar("Ошибка при поиске товара: ${e.message}"))
                launchMain {
                    onProductFound(null)
                }
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
        launchIO {
            val foundCount = results.count { it.product != null }
            val notFoundCount = results.size - foundCount

            Timber.i("Batch scanning finished: found $foundCount products, not found $notFoundCount")

            sendEvent(
                DynamicProductsEvent.ShowSnackbar(
                    "Сканирование завершено: найдено $foundCount, не найдено $notFoundCount"
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
            sendEvent(DynamicProductsEvent.ReturnSelectedProductId(selectedProduct.id))
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

            // Активно очищаем кэш для устаревших записей
            cleanExpiredSoundCache(currentTime)
        } else {
            Timber.d("Sound for barcode $barcode debounced. Last sound was ${currentTime - lastSoundTime}ms ago")
        }
    }

    // Метод для очистки устаревших записей в кэше звуков
    private fun cleanExpiredSoundCache(currentTime: Long) {
        soundPlayedCache.entries.removeIf { (_, timestamp) ->
            currentTime - timestamp > SOUND_DEBOUNCE_TIMEOUT
        }
    }

    fun onRefresh() {
        loadDynamicProducts()
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}