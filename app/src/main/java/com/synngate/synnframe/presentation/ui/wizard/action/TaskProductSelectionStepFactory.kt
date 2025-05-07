package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

class TaskProductSelectionStepFactory(
    private val productRepository: ProductRepository
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Состояние UI
        var manualProductCode by remember { mutableStateOf("") }
        var errorMessage by remember(context.validationError) {
            mutableStateOf(context.validationError)
        }
        var showCameraScannerDialog by remember { mutableStateOf(false) }
        var showProductSelectionDialog by remember { mutableStateOf(false) }

        // Состояние выбранного товара и его атрибутов
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var selectedStatus by remember { mutableStateOf(ProductStatus.STANDARD) }
        var expirationDate by remember { mutableStateOf<LocalDateTime?>(null) }

        // Проверяем, нужно ли отображать выбор срока годности (accountingModel = "BATCH")
        val needExpirationDate by remember(selectedProduct) {
            derivedStateOf {
                selectedProduct?.accountingModel == AccountingModel.BATCH
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Данные о продуктах из плана
        val plannedProduct = action.storageProduct?.product
        val planProducts = remember(action) {
            listOfNotNull(action.storageProduct)
        }
        val planProductIds = remember(planProducts) {
            planProducts.mapNotNull { it.product.id }.toSet()
        }

        // Вспомогательные функции
        fun createResultFromProduct(product: Product): TaskProduct {
            // Создаем TaskProduct с выбранными статусом и сроком годности
            return TaskProduct(
                product = product,
                quantity = 0f, // Количество будет задано на следующем шаге
                status = selectedStatus,
                expirationDate = expirationDate ?: LocalDateTime.of(1970, 1, 1, 0, 0)
            )
        }

        fun validateAndComplete(product: Product) {
            // Явная проверка без использования needExpirationDate
            val needsExpirationDate = product.accountingModel == AccountingModel.BATCH

            if (needsExpirationDate && expirationDate == null) {
                errorMessage = "Необходимо указать срок годности для данного товара"
                return
            }

            // Создаем TaskProduct и передаем в контекст
            val taskProduct = createResultFromProduct(product)
            context.onComplete(taskProduct)
        }

        val showError = { message: String ->
            errorMessage = message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        val searchProduct = { barcode: String ->
            if (barcode.isNotEmpty()) {
                errorMessage = null

                coroutineScope.launch {
                    processBarcodeForProduct(
                        barcode = barcode,
                        action = action,
                        onProductFound = { product ->
                            if (product != null) {
                                if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    selectedProduct = product

                                    // Устанавливаем значения по умолчанию для найденного продукта
                                    // Если продукт из плана, берем его статус и срок годности
                                    val plannedTaskProduct = planProducts.find { it.product.id == product.id }
                                    if (plannedTaskProduct != null) {
                                        selectedStatus = plannedTaskProduct.status
                                        expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                                            plannedTaskProduct.expirationDate
                                        } else null
                                    }
                                } else {
                                    showError("Продукт не соответствует плану")
                                }
                            } else {
                                showError("Продукт со штрихкодом '$barcode' не найден")
                            }
                        }
                    )
                }

                manualProductCode = ""
            }
        }

        // Обработчик штрихкодов от сканера устройства
        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                errorMessage = null

                coroutineScope.launch {
                    processBarcodeForProduct(
                        barcode = barcode,
                        action = action,
                        onProductFound = { product ->
                            if (product != null) {
                                if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    selectedProduct = product

                                    // Устанавливаем значения по умолчанию для найденного продукта
                                    val plannedTaskProduct = planProducts.find { it.product.id == product.id }
                                    if (plannedTaskProduct != null) {
                                        selectedStatus = plannedTaskProduct.status
                                        expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                                            plannedTaskProduct.expirationDate
                                        } else null
                                    }
                                } else {
                                    showError("Продукт не соответствует плану")
                                    setProcessingState(false)
                                }
                            } else {
                                showError("Продукт со штрихкодом '$barcode' не найден")
                                setProcessingState(false)
                            }
                        }
                    )
                }

                manualProductCode = ""
            }
        )

        // Обработка штрихкодов из контекста
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                searchProduct(barcode)
            }
        }

        // Инициализируем значения из контекста, если они есть
        LaunchedEffect(context.results[step.id]) {
            val result = context.results[step.id]
            if (result is TaskProduct && result.product != null) {
                selectedProduct = result.product
                selectedStatus = result.status
                expirationDate = if (result.hasExpirationDate()) result.expirationDate else null
            }
        }

        // Диалоги сканирования и выбора
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    searchProduct(barcode)
                    showCameraScannerDialog = false
                },
                onClose = {
                    showCameraScannerDialog = false
                },
                instructionText = if (plannedProduct != null)
                    stringResource(R.string.scan_product_expected, plannedProduct.name)
                else
                    stringResource(R.string.scan_product)
            )
        }

        if (showProductSelectionDialog) {
            OptimizedProductSelectionDialog(
                onProductSelected = { product ->
                    selectedProduct = product

                    // Устанавливаем значения по умолчанию для выбранного продукта
                    val plannedTaskProduct = planProducts.find { it.product.id == product.id }
                    if (plannedTaskProduct != null) {
                        selectedStatus = plannedTaskProduct.status
                        expirationDate = if (plannedTaskProduct.hasExpirationDate()) {
                            plannedTaskProduct.expirationDate
                        } else null
                    }

                    showProductSelectionDialog = false
                },
                onDismiss = {
                    showProductSelectionDialog = false
                },
                initialFilter = "",
                title = "Выберите товар",
                planProductIds = planProductIds.ifEmpty { null }
            )
        }

        // Основное содержимое UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок и описание действия
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Сообщение об ошибке валидации
            if (errorMessage != null) {
                ValidationErrorMessage(
                    message = errorMessage!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Поле поиска продукта
            ProductSearchBar(
                value = manualProductCode,
                onValueChange = {
                    manualProductCode = it
                    errorMessage = null
                },
                onSearch = { searchProduct(manualProductCode) },
                onScannerClick = { showCameraScannerDialog = true },
                errorMessage = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранного продукта и дополнительных полей
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный товар:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProductCard(
                    product = selectedProduct!!,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор статуса товара
                ProductStatusSelector(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    isRequired = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор срока годности (если требуется)
                if (selectedProduct != null && selectedProduct?.accountingModel == AccountingModel.BATCH) {
                    ExpirationDatePicker(
                        expirationDate = expirationDate,
                        onDateSelected = { expirationDate = it },
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Кнопка подтверждения
                Button(
                    onClick = { validateAndComplete(selectedProduct!!) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Подтвердить")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Список продуктов из плана
            if (planProducts.isNotEmpty()) {
                PlanProductsList(
                    planProducts = planProducts,
                    onProductSelect = { taskProduct ->
                        selectedProduct = taskProduct.product
                        selectedStatus = taskProduct.status
                        expirationDate = if (taskProduct.hasExpirationDate()) {
                            taskProduct.expirationDate
                        } else null
                    }
                )
            }

            // Кнопка выбора из списка
            Button(
                onClick = { showProductSelectionDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Выбрать из списка товаров")
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        if (value !is TaskProduct) return false

        // Проверяем наличие product и status
        if (value.product == null || value.status == null) return false

        // Проверяем наличие срока годности для товаров с учетом по партиям
        if (value.product.accountingModel == AccountingModel.BATCH && !value.hasExpirationDate()) {
            return false
        }

        return true
    }

    /**
     * Обработка штрихкода для поиска продукта
     */
    private suspend fun processBarcodeForProduct(
        barcode: String,
        action: PlannedAction,
        onProductFound: (Product?) -> Unit
    ) {
        val plannedProduct = action.storageProduct?.product

        // Сначала проверяем, соответствует ли штрихкод планируемому продукту
        if (plannedProduct != null) {
            val plannedBarcodes = plannedProduct.getAllBarcodes()
            if (plannedBarcodes.contains(barcode)) {
                onProductFound(plannedProduct)
                return
            }
        }

        // Если продукт не найден в плане, ищем в репозитории
        try {
            val product = withContext(Dispatchers.IO) {
                productRepository.findProductByBarcode(barcode)
            }

            onProductFound(product)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
            onProductFound(null)
        }
    }
}