package com.synngate.synnframe.presentation.ui.dynamicmenu.product

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.mapper.DynamicProductMapper
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductDetailEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductDetailState
import com.synngate.synnframe.presentation.ui.products.mapper.ProductUiMapper
import com.synngate.synnframe.presentation.ui.products.model.BarcodeUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductUnitUiModel
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.resources.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DynamicProductDetailViewModel(
    dynamicProduct: DynamicProduct,
    private val clipboardService: ClipboardService,
    private val productUiMapper: ProductUiMapper,
    private val resourceProvider: ResourceProvider
) : BaseViewModel<DynamicProductDetailState, DynamicProductDetailEvent>(
    DynamicProductDetailState(product = dynamicProduct)
) {
    // Кэшированные UI-модели для улучшения производительности
    private var productUiModel: ProductDetailUiModel? = null
    private var selectedUnitUiModels: List<ProductUnitUiModel> = emptyList()
    private var selectedUnitBarcodes: List<BarcodeUiModel> = emptyList()

    init {
        // При инициализации сразу создаем UI-модель и выбираем основную единицу измерения
        initializeUiModels()
    }

    private fun initializeUiModels() {
        val product = uiState.value.product ?: return

        // Преобразуем динамический товар в обычный для переиспользования существующих маппинг-функций
        val mappedProduct = DynamicProductMapper.toProduct(product)
        productUiModel = productUiMapper.mapToDetailModel(mappedProduct)

        // Выбираем основную единицу измерения или первую доступную
        val unitIdToSelect = if (mappedProduct.units.any { it.id == product.mainUnitId }) {
            product.mainUnitId
        } else {
            mappedProduct.units.firstOrNull()?.id ?: ""
        }
        updateSelectedUnit(unitIdToSelect, mappedProduct)

        updateState { it.copy(selectedUnitId = unitIdToSelect) }
    }

    fun selectUnit(unitId: String) {
        updateSelectedUnit(unitId)
        updateState { it.copy(selectedUnitId = unitId) }
    }

    private fun updateSelectedUnit(unitId: String, mappedProduct: Product? = null) {
        val product = uiState.value.product ?: return
        val currentMappedProduct = mappedProduct ?: DynamicProductMapper.toProduct(product)

        // Фильтруем UI-модели единиц измерения для выбранной
        selectedUnitUiModels = productUiModel?.units?.filter { it.id == unitId } ?: emptyList()

        // Получаем штрихкоды для выбранной единицы измерения
        val selectedUnit = currentMappedProduct.units.find { it.id == unitId }

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

            sendEvent(DynamicProductDetailEvent.CopyBarcodeToClipboard(barcode))
            sendEvent(DynamicProductDetailEvent.ShowSnackbar("Штрихкод скопирован в буфер обмена"))
        }
    }

    fun copyProductInfoToClipboard() {
        val product = uiState.value.product ?: return

        val accountingModelText = when (product.getAccountingModelEnum()) {
            AccountingModel.BATCH -> "By batches"
            AccountingModel.QTY -> "By count"
        }

        val mainUnit = product.getMainUnit()?.name ?: "Unknown"

        val productInfo = """
            Name: ${product.name}
            ID: ${product.id}
            Article: ${product.articleNumber}
            Account model: $accountingModelText
            Base UOM: $mainUnit
            UOMs: ${product.units.joinToString("\n") { it.name }}
            Barcodes: ${product.units.flatMap { listOf(it.mainBarcode) + it.barcodes }.distinct().joinToString("\n")}
        """.trimIndent()

        val isCopied = clipboardService.copyToClipboard(
            text = productInfo,
            label = "Product info"
        )

        if (isCopied) {
            updateState { it.copy(isInfoCopied = true) }

            // Автоматически сбрасываем флаг через некоторое время
            viewModelScope.launch {
                delay(2000) // 2 секунды
                updateState { it.copy(isInfoCopied = false) }
            }

            sendEvent(DynamicProductDetailEvent.CopyProductInfoToClipboard)
            sendEvent(DynamicProductDetailEvent.ShowSnackbar("Информация о товаре скопирована в буфер обмена"))
        }
    }

    // Метод для получения UI-модели товара
    fun getProductUiModel(): ProductDetailUiModel? {
        if (productUiModel == null) {
            val product = uiState.value.product ?: return null
            val mappedProduct = DynamicProductMapper.toProduct(product)
            productUiModel = productUiMapper.mapToDetailModel(mappedProduct)
        }
        return productUiModel
    }

    // Методы для получения UI-моделей единиц измерения
    fun getSelectedUnitUiModels(): List<ProductUnitUiModel> = selectedUnitUiModels

    // Метод для получения UI-моделей штрихкодов
    fun getAllBarcodesUiModels(): List<BarcodeUiModel> = selectedUnitBarcodes

    // Метод для возврата на предыдущий экран
    fun navigateBack() {
        sendEvent(DynamicProductDetailEvent.NavigateBack)
    }
}