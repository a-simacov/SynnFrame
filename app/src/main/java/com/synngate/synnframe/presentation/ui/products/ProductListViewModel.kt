package com.synngate.synnframe.presentation.ui.product

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.di.ProductListViewModel
import com.synngate.synnframe.presentation.ui.product.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.product.model.ProductListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProductListViewModel(
    private val productUseCases: ProductUseCases,
    private val isSelectionMode: Boolean = false,
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

    /**
     * Наблюдает за количеством товаров
     */
    private fun observeProductsCount() {
        launchIO {
            productUseCases.getProductsCount().collectLatest { count ->
                updateState { it.copy(productsCount = count) }
            }
        }
    }

    /**
     * Загружает товары
     */
    private fun loadProducts() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                productUseCases.getProducts().collectLatest { products ->
                    updateState { state ->
                        state.copy(
                            products = products,
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

    /**
     * Обновляет поисковый запрос и фильтрует товары
     */
    fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }

        // Отменяем предыдущий поиск, если он выполняется
        searchJob?.cancel()

        searchJob = launchIO {
            if (query.isEmpty()) {
                loadProducts()
            } else {
                updateState { it.copy(isLoading = true) }

                try {
                    // Используем debounce для предотвращения частых запросов при быстром вводе
                    productUseCases.getProductsByNameFilter(query)
                        .debounce(300)
                        .collectLatest { products ->
                            updateState { state ->
                                state.copy(
                                    products = products,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
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
    }

    /**
     * Обрабатывает нажатие на товар
     */
    fun onProductClick(product: Product) {
        if (uiState.value.isSelectionMode) {
            // В режиме выбора товара возвращаемся к заданию с выбранным товаром
            sendEvent(ProductListEvent.ReturnToTaskWithProduct(product))
        } else {
            // В обычном режиме переходим к деталям товара
            sendEvent(ProductListEvent.NavigateToProductDetail(product.id))
        }
    }

    /**
     * Синхронизирует товары с сервером
     */
    fun syncProducts() {
        launchIO {
            updateState { it.copy(isSyncing = true) }

            try {
                val result = productUseCases.syncProductsWithServer()

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    val formattedTime = LocalDateTime.now().format(dateFormatter)

                    updateState { it.copy(
                        isSyncing = false,
                        lastSyncTime = formattedTime
                    ) }

                    sendEvent(ProductListEvent.ShowSnackbar("Синхронизация выполнена. Обновлено $count товаров."))
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"

                    updateState { it.copy(isSyncing = false) }
                    sendEvent(ProductListEvent.ShowSnackbar("Ошибка синхронизации: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing products")

                updateState { it.copy(isSyncing = false) }
                sendEvent(ProductListEvent.ShowSnackbar("Ошибка синхронизации: ${e.message}"))
            }
        }
    }

    /**
     * Возвращается на предыдущий экран
     */
    fun navigateBack() {
        sendEvent(ProductListEvent.NavigateBack)
    }
}