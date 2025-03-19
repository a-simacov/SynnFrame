// Файл: com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel.kt

package com.synngate.synnframe.presentation.ui.products

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.ProductUnit
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.di.ClearableViewModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для экрана деталей товара
 */
class ProductDetailViewModel(
    private val productId: String,
    private val productUseCases: ProductUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher
) : ClearableViewModel() {

    private val _state = MutableStateFlow(ProductDetailState())
    val state: StateFlow<ProductDetailState> = _state.asStateFlow()

    init {
        loadProduct()
    }

    /**
     * Загрузка данных товара
     */
    fun loadProduct() {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val product = productUseCases.getProductById(productId)

                if (product != null) {
                    _state.update { it.copy(
                        product = product,
                        // По умолчанию выбираем основную единицу измерения
                        selectedUnitId = product.mainUnitId,
                        isLoading = false,
                        error = null
                    ) }

                    loggingService.logInfo("Просмотр товара: ${product.name} (${product.id})")
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Товар с ID $productId не найден"
                    ) }

                    loggingService.logWarning("Попытка просмотра несуществующего товара с ID $productId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product with ID $productId")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка загрузки товара: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка загрузки товара с ID $productId: ${e.message}")
                }
            }
        }
    }

    /**
     * Выбор единицы измерения
     */
    fun selectUnit(unitId: String) {
        _state.update { it.copy(selectedUnitId = unitId) }
    }

    /**
     * Переключение отображения панели штрихкодов
     */
    fun toggleBarcodesPanel() {
        _state.update { it.copy(showBarcodes = !it.showBarcodes) }
    }

    /**
     * Получение текущей выбранной единицы измерения
     */
    fun getSelectedUnit(): ProductUnit? {
        val currentState = state.value
        val product = currentState.product ?: return null
        val selectedUnitId = currentState.selectedUnitId ?: return null

        return product.units.find { it.id == selectedUnitId }
    }

    /**
     * Получение основной единицы измерения
     */
    fun getMainUnit(): ProductUnit? {
        val product = state.value.product ?: return null
        return product.getMainUnit()
    }

    /**
     * Проверка, является ли указанная единица измерения основной
     */
    fun isMainUnit(unitId: String): Boolean {
        val product = state.value.product ?: return false
        return product.mainUnitId == unitId
    }

    /**
     * Получение наименования модели учета для отображения
     */
    fun getAccountingModelName(accountingModel: AccountingModel): String {
        return when (accountingModel) {
            AccountingModel.BATCH -> "По партиям и количеству"
            AccountingModel.QTY -> "Только по количеству"
        }
    }

    /**
     * Получение всех штрихкодов товара (из всех единиц измерения)
     */
    fun getAllBarcodes(): List<String> {
        val product = state.value.product ?: return emptyList()
        return product.getAllBarcodes()
    }

    /**
     * Получение штрихкодов для выбранной единицы измерения
     */
    fun getSelectedUnitBarcodes(): List<String> {
        val selectedUnit = getSelectedUnit() ?: return emptyList()
        return selectedUnit.allBarcodes
    }

    /**
     * Проверка на основной штрихкод
     */
    fun isMainBarcode(barcode: String): Boolean {
        val selectedUnit = getSelectedUnit() ?: return false
        return selectedUnit.mainBarcode == barcode
    }
}