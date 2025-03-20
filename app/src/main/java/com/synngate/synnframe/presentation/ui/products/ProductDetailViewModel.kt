package com.synngate.synnframe.presentation.ui.products

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для экрана деталей товара
 */
class ProductDetailViewModel(
    private val productId: String,
    private val productUseCases: ProductUseCases,
    private val loggingService: LoggingService,
    private val clipboardService: ClipboardService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<ProductDetailState, ProductDetailEvent>(
    ProductDetailState()
) {

    init {
        loadProduct()
    }

    /**
     * Загрузка данных товара
     */
    fun loadProduct() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val product = productUseCases.getProductById(productId)

                if (product != null) {
                    // Выбираем основную единицу измерения по умолчанию
                    val mainUnitId = product.mainUnitId

                    updateState {
                        it.copy(
                            product = product,
                            selectedUnitId = mainUnitId,
                            isLoading = false,
                            error = null
                        )
                    }

                    loggingService.logInfo("Просмотр товара: ${product.name} (${product.id})")
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Товар с ID $productId не найден"
                        )
                    }

                    loggingService.logWarning("Попытка просмотра несуществующего товара с ID $productId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product with ID $productId")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки товара: ${e.message}"
                    )
                }

                loggingService.logError("Ошибка загрузки товара с ID $productId: ${e.message}")
            }
        }
    }

    /**
     * Выбор единицы измерения
     */
    fun selectUnit(unitId: String) {
        updateState { it.copy(selectedUnitId = unitId) }
    }

    /**
     * Переключение отображения панели штрихкодов
     */
    fun toggleBarcodesPanel() {
        updateState { it.copy(showBarcodes = !it.showBarcodes) }
        sendEvent(ProductDetailEvent.ToggleBarcodesPanel)
    }

    /**
     * Переключение отображения расширенной информации о единицах измерения
     */
    fun toggleExtendedUnitInfo() {
        updateState { it.copy(showExtendedUnitInfo = !it.showExtendedUnitInfo) }
    }

    /**
     * Копирование штрихкода в буфер обмена
     */
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

    /**
     * Копирование информации о товаре в буфер обмена
     */
    fun copyProductInfoToClipboard() {
        val product = uiState.value.product ?: return

        val accountingModelText = when (product.accountingModel) {
            AccountingModel.BATCH -> "По партиям и количеству"
            AccountingModel.QTY -> "Только по количеству"
        }

        val mainUnit = product.getMainUnit()?.name ?: "Не указана"

        val productInfo = """
            Наименование: ${product.name}
            Артикул: ${product.articleNumber}
            Модель учета: $accountingModelText
            Основная единица измерения: $mainUnit
            Количество единиц измерения: ${product.units.size}
            Общее количество штрихкодов: ${product.getAllBarcodes().size}
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

    /**
     * Получение текущей выбранной единицы измерения
     */
    fun getSelectedUnit(): ProductUnit? {
        val currentState = uiState.value
        val product = currentState.product ?: return null
        val selectedUnitId = currentState.selectedUnitId ?: return null

        return product.units.find { it.id == selectedUnitId }
    }

    /**
     * Получение основной единицы измерения
     */
    fun getMainUnit(): ProductUnit? {
        val product = uiState.value.product ?: return null
        return product.getMainUnit()
    }

    /**
     * Проверка, является ли указанная единица измерения основной
     */
    fun isMainUnit(unitId: String): Boolean {
        val product = uiState.value.product ?: return false
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
        val product = uiState.value.product ?: return emptyList()
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

    /**
     * Возврат к списку товаров
     */
    fun navigateBack() {
        sendEvent(ProductDetailEvent.NavigateBack)
    }

    /**
     * Выполняет поиск товара по штрихкоду
     * @return Найденный товар или null, если товар не найден
     */
    suspend fun findProductByBarcode(barcode: String): Product? {
        return try {
            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                loggingService.logInfo("Найден товар по штрихкоду $barcode: ${product.name}")
            } else {
                loggingService.logWarning("Товар по штрихкоду $barcode не найден")
            }

            product
        } catch (e: Exception) {
            Timber.e(e, "Error finding product by barcode $barcode")
            loggingService.logError("Ошибка поиска товара по штрихкоду $barcode: ${e.message}")
            null
        }
    }

    /**
     * Открывает экран для сканирования штрихкода
     */
    fun openBarcodeScanner() {
        updateState { it.copy(showBarcodeScanner = true) }
    }

    /**
     * Закрывает экран для сканирования штрихкода
     */
    fun closeBarcodeScanner() {
        updateState { it.copy(showBarcodeScanner = false) }
    }

    /**
     * Обрабатывает результат сканирования штрихкода
     */
    fun handleScannedBarcode(barcode: String, onProductFound: (Product) -> Unit) {
        launchIO {
            updateState { it.copy(isLoading = true) }

            val product = findProductByBarcode(barcode)

            if (product != null) {
                // Если товар найден, вызываем колбэк
                launchMain { onProductFound(product) }
            } else {
                // Если товар не найден, просто показываем сообщение
                sendEvent(ProductDetailEvent.ShowSnackbar(
                    "Товар по штрихкоду $barcode не найден"
                ))
            }

            updateState { it.copy(
                isLoading = false,
                showBarcodeScanner = false
            ) }
        }
    }
}