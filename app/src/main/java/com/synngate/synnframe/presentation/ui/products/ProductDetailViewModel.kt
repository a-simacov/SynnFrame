package com.synngate.synnframe.presentation.ui.products

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.BarcodeUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailState
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductUnitUiModel
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.resources.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ProductDetailViewModel(
    private val productId: String,
    private val productUseCases: ProductUseCases,
    private val clipboardService: ClipboardService,
    private val productUiMapper: ProductUiMapper,
    private val resourceProvider: ResourceProvider,
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
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = resourceProvider.getString(R.string.product_not_found, productId)
                        )
                    }

                    Timber.w("Attempt to view unexistent ID $productId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product with ID $productId: ${e.message}")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = resourceProvider.getString(R.string.error_loading_product, e.message ?: "")
                    )
                }
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
            label = "Product barcode"
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
            sendEvent(ProductDetailEvent.ShowSnackbar("Barcode copied to clipboard"))
        }
    }

    fun copyProductInfoToClipboard() {
        val product = uiState.value.product ?: return

        val accountingModelText = when (product.accountingModel) {
            AccountingModel.BATCH -> "By batches and quantity"
            AccountingModel.QTY -> "By quantity only"
        }

        val mainUnit = product.getMainUnit()?.name ?: "Not specified"

        val productInfo = """
            Name: ${product.name}
            ID: ${product.id}
            Article: ${product.articleNumber}
            Accounting model: $accountingModelText
            Main unit of measurement: $mainUnit
            Units of measurement: ${product.units.joinToString("\n") { it.name }}
            Barcodes: ${product.getAllBarcodes().distinct().joinToString("\n")}
        """.trimIndent()

        val isCopied = clipboardService.copyToClipboard(
            text = productInfo,
            label = "Product information"
        )

        if (isCopied) {
            updateState { it.copy(isInfoCopied = true) }

            // Автоматически сбрасываем флаг через некоторое время
            viewModelScope.launch {
                delay(2000) // 2 секунды
                updateState { it.copy(isInfoCopied = false) }
            }

            sendEvent(ProductDetailEvent.CopyProductInfoToClipboard)
            sendEvent(ProductDetailEvent.ShowSnackbar("Product information copied to clipboard"))
        }
    }

    fun getProductUiModel(): ProductDetailUiModel? = productUiModel

    fun getSelectedUnitUiModels(): List<ProductUnitUiModel> = selectedUnitUiModels

    fun getAllBarcodesUiModels(): List<BarcodeUiModel> = productUiModel?.barcodes ?: emptyList()

    fun navigateBack() {
        sendEvent(ProductDetailEvent.NavigateBack)
    }
}