package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Фабрика для создания компонента шага выбора товара с указанием количества
 */
class QuantityProductSelectionStepFactory(
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

        // Состояние для выбранного товара и количества
        var selectedProduct by remember { mutableStateOf<TaskProduct?>(null) }
        var quantity by remember { mutableStateOf("") }

        // Состояние готовности к завершению шага
        var isReadyToComplete by remember { mutableStateOf(false) }

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

        // Текущий выбранный продукт из контекста
        val contextProduct = remember(context.results) {
            context.results[step.id] as? TaskProduct
        }

        // Инициализация состояния из контекста, если есть результат
        LaunchedEffect(contextProduct) {
            if (contextProduct != null && selectedProduct == null) {
                selectedProduct = contextProduct
                quantity = contextProduct.quantity.toString()
            }
        }

        // Вспомогательные функции
        val showError = { message: String ->
            errorMessage = message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        val completeStep = {
            if (selectedProduct != null && quantity.isNotEmpty()) {
                try {
                    val quantityValue = quantity.toFloatOrNull() ?: 0f
                    if (quantityValue <= 0f) {
                        showError("Количество должно быть больше нуля")
                    } else {
                        // Создаем копию TaskProduct с обновленным количеством
                        val result = selectedProduct!!.copy(quantity = quantityValue)
                        context.onComplete(result)
                    }
                } catch (e: Exception) {
                    showError("Ошибка при обработке количества: ${e.localizedMessage}")
                }
            } else if (selectedProduct == null) {
                showError("Необходимо выбрать товар")
            } else {
                showError("Необходимо указать количество")
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
                                // Находим запланированный продукт, соответствующий найденному
                                val plannedTaskProduct = planProducts.find { it.product.id == product.id }

                                if (plannedTaskProduct != null) {
                                    selectedProduct = plannedTaskProduct
                                    quantity = plannedTaskProduct.quantity.toString()
                                    isReadyToComplete = true
                                } else if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    // Если не нашли в плане, но товар разрешен (по правилам)
                                    selectedProduct = TaskProduct(product = product, quantity = 1f)
                                    quantity = "1"
                                    isReadyToComplete = true
                                } else {
                                    showError("Товар не соответствует плану")
                                }
                            } else {
                                showError("Товар со штрихкодом '$barcode' не найден")
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
                                // Находим запланированный продукт, соответствующий найденному
                                val plannedTaskProduct = planProducts.find { it.product.id == product.id }

                                if (plannedTaskProduct != null) {
                                    selectedProduct = plannedTaskProduct
                                    quantity = plannedTaskProduct.quantity.toString()
                                    isReadyToComplete = true
                                } else if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    // Если не нашли в плане, но товар разрешен (по правилам)
                                    selectedProduct = TaskProduct(product = product, quantity = 1f)
                                    quantity = "1"
                                    isReadyToComplete = true
                                } else {
                                    showError("Товар не соответствует плану")
                                    setProcessingState(false)
                                }
                            } else {
                                showError("Товар со штрихкодом '$barcode' не найден")
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
                    // Находим запланированный продукт, соответствующий выбранному
                    val plannedTaskProduct = planProducts.find { it.product.id == product.id }

                    if (plannedTaskProduct != null) {
                        selectedProduct = plannedTaskProduct
                        quantity = plannedTaskProduct.quantity.toString()
                    } else {
                        selectedProduct = TaskProduct(product = product, quantity = 1f)
                        quantity = "1"
                    }
                    isReadyToComplete = true
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

            // Отображение выбранного продукта и поле для ввода количества, если продукт выбран
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный товар:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedProduct!!.product.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Поле для ввода количества
                        QuantityTextField(
                            value = quantity,
                            onValueChange = {
                                quantity = it
                                isReadyToComplete = it.isNotEmpty()
                            },
                            label = "Количество",
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage != null && errorMessage!!.contains("количество", ignoreCase = true)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Кнопка для подтверждения выбора товара и количества
                        Button(
                            onClick = { completeStep() },
                            enabled = isReadyToComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Подтвердить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Список товаров из плана
            if (planProducts.isNotEmpty()) {
                PlanProductsList(
                    planProducts = planProducts,
                    onProductSelect = { taskProduct ->
                        selectedProduct = taskProduct
                        quantity = taskProduct.quantity.toString()
                        isReadyToComplete = true
                    },
                    showQuantity = true // Показываем количество из плана
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
            ActionObjectType.TASK_PRODUCT -> {
                val taskProduct = value as? TaskProduct
                taskProduct != null && taskProduct.quantity > 0
            }
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

/**
 * Дополненная версия компонента для отображения списка продуктов из плана
 * с возможностью отображения количества
 */
@Composable
fun PlanProductsList(
    planProducts: List<TaskProduct>,
    onProductSelect: (TaskProduct) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "По плану:",
    maxHeight: Int = 150,
    showDivider: Boolean = true,
    showQuantity: Boolean = false
) {
    if (planProducts.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight.dp)
        ) {
            items(planProducts) { taskProduct ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onProductSelect(taskProduct) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = taskProduct.product.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (taskProduct.product.articleNumber.isNotEmpty()) {
                                Text(
                                    text = "Артикул: ${taskProduct.product.articleNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (showQuantity && taskProduct.quantity > 0) {
                                Text(
                                    text = "Количество в плане: ${taskProduct.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        IconButton(onClick = { onProductSelect(taskProduct) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Выбрать",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}