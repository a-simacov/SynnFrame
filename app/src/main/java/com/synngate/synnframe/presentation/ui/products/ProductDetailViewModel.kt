package com.synngate.synnframe.presentation.ui.products

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.BarcodeUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailState
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductUnitUiModel
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.resources.ResourceProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ProductDetailViewModel(
    private val productId: String,
    private val productUseCases: ProductUseCases,
    private val loggingService: LoggingService,
    private val clipboardService: ClipboardService,
    private val productUiMapper: ProductUiMapper,
    private val resourceProvider: ResourceProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ProductDetailState, ProductDetailEvent>(
    ProductDetailState()
) {

    private var productUiModel: ProductDetailUiModel? = null
    private var selectedUnitUiModels: List<ProductUnitUiModel> = emptyList()
    private var selectedUnitBarcodes: List<BarcodeUiModel> = emptyList()

    init {
        loadProduct()
    }

    fun loadProduct() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val product = productUseCases.getProductById(productId)

                if (product != null) {
                    // Маппинг доменной модели в UI-модель
                    productUiModel = productUiMapper.mapToDetailModel(product)

                    updateSelectedUnit(product.mainUnitId)

                    updateState {
                        it.copy(
                            product = product,
                            selectedUnitId = product.mainUnitId,
                            isLoading = false,
                            error = null
                        )
                    }

                    loggingService.logInfo("Просмотр товара: ${product.name} (${product.id})")
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = resourceProvider.getString(R.string.product_not_found, productId)
                        )
                    }

                    loggingService.logWarning("Попытка просмотра несуществующего товара с ID $productId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product with ID $productId")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = resourceProvider.getString(R.string.error_loading_product, e.message ?: "")
                    )
                }

                loggingService.logError("Ошибка загрузки товара с ID $productId: ${e.message}")
            }
        }
    }

    fun selectUnit(unitId: String) {
        updateSelectedUnit(unitId)
        updateState { it.copy(selectedUnitId = unitId) }
    }

    private fun updateSelectedUnit(unitId: String) {
        val product = uiState.value.product ?: return

        // Фильтруем UI-модели единиц измерения для выбранной
        selectedUnitUiModels = productUiModel?.units?.filter { it.id == unitId } ?: emptyList()

        // Получаем штрихкоды для выбранной единицы измерения
        val selectedUnit = product.units.find { it.id == unitId }
        selectedUnitBarcodes = selectedUnit?.let { unit ->
            unit.allBarcodes.map { barcode ->
                BarcodeUiModel(barcode, isMainBarcode = barcode == unit.mainBarcode)
            }
        } ?: emptyList()
    }

    fun copyBarcodeToClipboard(barcode: String) {
        val isCopied = clipboardService.copyToClipboard(
            text = barcode,
            label = "Штрихкод товара"
        )

        if (isCopied) {
            updateState { it.copy(
                isInfoCopied = true,
                lastCopiedBarcode = barcode
            ) }

            // Автоматически сбрасываем флаг через некоторое время
            viewModelScope.launch {
                delay(2000) // 2 секунды
                updateState { it.copy(isInfoCopied = false) }
            }

            sendEvent(ProductDetailEvent.CopyBarcodeToClipboard(barcode))
            sendEvent(ProductDetailEvent.ShowSnackbar("Штрихкод скопирован в буфер обмена"))

            viewModelScope.launch(ioDispatcher) {
                loggingService.logInfo("Штрихкод товара скопирован в буфер обмена: $barcode")
            }
        }
    }

    fun copyProductInfoToClipboard() {
        val product = uiState.value.product ?: return

        val accountingModelText = when (product.accountingModel) {
            AccountingModel.BATCH -> "По партиям и количеству"
            AccountingModel.QTY -> "Только по количеству"
        }

        val mainUnit = product.getMainUnit()?.name ?: "Не указана"

        val productInfo = """
            Наименование: ${product.name}
            Идентификатор: ${product.id}
            Артикул: ${product.articleNumber}
            Модель учета: $accountingModelText
            Основная единица измерения: $mainUnit
            Единицы измерения: ${product.units.joinToString("\n") { it.name }}
            Штрихкоды: ${product.getAllBarcodes().distinct().joinToString("\n")}
        """.trimIndent()

        val isCopied = clipboardService.copyToClipboard(
            text = productInfo,
            label = "Информация о товаре"
        )

        if (isCopied) {
            updateState { it.copy(isInfoCopied = true) }

            // Автоматически сбрасываем флаг через некоторое время
            viewModelScope.launch {
                delay(2000) // 2 секунды
                updateState { it.copy(isInfoCopied = false) }
            }

            sendEvent(ProductDetailEvent.CopyProductInfoToClipboard)
            sendEvent(ProductDetailEvent.ShowSnackbar("Информация о товаре скопирована в буфер обмена"))

            viewModelScope.launch(ioDispatcher) {
                loggingService.logInfo("Информация о товаре скопирована в буфер обмена: ${product.name}")
            }
        }
    }

    fun getProductUiModel(): ProductDetailUiModel? = productUiModel

    fun getSelectedUnitUiModels(): List<ProductUnitUiModel> = selectedUnitUiModels

    fun getAllBarcodesUiModels(): List<BarcodeUiModel> = productUiModel?.barcodes ?: emptyList()

    fun navigateBack() {
        sendEvent(ProductDetailEvent.NavigateBack)
    }
}