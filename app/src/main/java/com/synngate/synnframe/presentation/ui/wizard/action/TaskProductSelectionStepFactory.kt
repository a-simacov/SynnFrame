package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

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

        // Текущий выбранный продукт
        val selectedProduct = remember(context.results) {
            context.results[step.id] as? Product ?:
            (context.results[step.id] as? TaskProduct)?.product
        }

        // Вспомогательные функции
        fun createResultFromProduct(product: Product): Any {
            // Создаем TaskProduct с нулевым количеством
            // Количество будет задано на следующем шаге
            return if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                TaskProduct(product = product, quantity = 0f)
            } else {
                product
            }
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
                                    context.onComplete(createResultFromProduct(product))
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
                                    context.onComplete(createResultFromProduct(product))
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
                    context.onComplete(createResultFromProduct(product))
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок и описание действия
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Сообщение об ошибке валидации
            if (context.validationError != null) {
                ValidationErrorMessage(
                    message = context.validationError,
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

            // Отображение выбранного продукта, если есть
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный товар:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProductCard(
                    product = selectedProduct,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Список продуктов из плана
            if (planProducts.isNotEmpty()) {
                PlanProductsList(
                    planProducts = planProducts,
                    onProductSelect = { taskProduct ->
                        context.onComplete(
                            if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                taskProduct.copy(quantity = 0f) // Сбрасываем количество в 0
                            } else {
                                taskProduct.product
                            }
                        )
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
        return when (step.objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> value is Product
            ActionObjectType.TASK_PRODUCT -> value is TaskProduct
            else -> false
        }
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