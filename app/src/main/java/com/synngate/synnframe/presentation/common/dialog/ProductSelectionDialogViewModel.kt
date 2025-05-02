package com.synngate.synnframe.presentation.common.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Состояние UI для диалога выбора продукта
 */
data class ProductSelectionUiState(
    val searchQuery: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showScanner: Boolean = false
)

/**
 * ViewModel для диалога выбора продукта
 * Инкапсулирует бизнес-логику и управляет состоянием
 */
class ProductSelectionDialogViewModel(
    private val productRepository: ProductRepository,
    private val planProductIds: Set<String>? = null
) : ViewModel() {

    // Внутреннее состояние
    private val _uiState = MutableStateFlow(ProductSelectionUiState())
    val uiState: StateFlow<ProductSelectionUiState> = _uiState.asStateFlow()

    // Поток для поискового запроса
    private val _searchQuery = MutableStateFlow("")

    // Текущий поисковый Job для возможности отмены
    private var searchJob: Job? = null

    init {
        // Запускаем наблюдение за запросами поиска
        observeSearchQueries()

        // Загружаем продукты по плану, если они указаны
        if (planProductIds != null && planProductIds.isNotEmpty()) {
            loadPlanProducts(planProductIds)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearchQueries() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Предотвращаем частые запросы при быстром вводе
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isEmpty() && planProductIds.isNullOrEmpty()) {
                        // Если запрос пустой и нет планируемых продуктов - возвращаем пустой список
                        MutableStateFlow(emptyList())
                    } else if (query.isEmpty() && !planProductIds.isNullOrEmpty()) {
                        // Если запрос пустой, но есть планируемые продукты - используем их
                        // Оборачиваем результат в MutableStateFlow, так как getProductsByIds возвращает List, а не Flow
                        MutableStateFlow(productRepository.getProductsByIds(planProductIds))
                    } else {
                        // Фильтруем по запросу
                        productRepository.getProductsByNameFilter(query)
                            .map { products ->
                                // Применяем дополнительную фильтрацию по планируемым продуктам, если они заданы
                                if (!planProductIds.isNullOrEmpty()) {
                                    products.filter { planProductIds.contains(it.id) }
                                } else {
                                    products
                                }
                            }
                    }
                }
                .collect { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false
                    )
                }
        }
    }

    /**
     * Загружает продукты из плана
     */
    private fun loadPlanProducts(productIds: Set<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val products = productRepository.getProductsByIds(productIds)
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке продуктов из плана")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    /**
     * Обновляет поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQuery.value = query
    }

    /**
     * Поиск продукта по штрихкоду
     */
    fun findProductByBarcode(barcode: String, onFound: (Product?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val product = productRepository.findProductByBarcode(barcode)

                // Проверяем, что продукт входит в планируемые, если они заданы
                val isValidProduct = product != null &&
                        (planProductIds == null || planProductIds.isEmpty() || planProductIds.contains(product.id))

                _uiState.value = _uiState.value.copy(isLoading = false)

                onFound(if (isValidProduct) product else null)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка поиска: ${e.message}"
                )
                onFound(null)
            }
        }
    }

    /**
     * Показывает/скрывает сканер штрихкодов
     */
    fun setShowScanner(show: Boolean) {
        _uiState.value = _uiState.value.copy(showScanner = show)
    }

    /**
     * Очищает ошибку
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Factory для создания ViewModel с внедрением зависимостей
     */
    class Factory(
        private val productRepository: ProductRepository,
        private val planProductIds: Set<String>? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductSelectionDialogViewModel::class.java)) {
                return ProductSelectionDialogViewModel(productRepository, planProductIds) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}