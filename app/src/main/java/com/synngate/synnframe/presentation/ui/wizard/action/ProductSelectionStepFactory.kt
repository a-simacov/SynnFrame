package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.ProductItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора продукта с тремя способами ввода
 */
class ProductSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel,
    private val navController: NavController? = null
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }

        // Состояния для диалогов и режимов ввода
        var showCameraScannerDialog by remember { mutableStateOf(false) }
        var showScanMethodSelection by remember { mutableStateOf(false) }
        var selectedInputMethod by remember { mutableStateOf(InputMethod.NONE) }

        // Запланированный продукт
        val plannedProduct = action.storageProduct?.product

        // Получение данных из ViewModel
        val products by wizardViewModel.products.collectAsState()

        // Получаем сервис сканера для встроенного сканера
        val scannerService = LocalScannerService.current

        // Список продуктов из плана для показа пользователю
        val planProducts = remember(action) {
            listOfNotNull(action.storageProduct)
        }

        // Получаем уже выбранный продукт из контекста, если есть
        val selectedProduct = remember(context.results) {
            context.results[step.id] as? Product ?:
            (context.results[step.id] as? TaskProduct)?.product
        }

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (barcode != null && barcode.isNotEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")

                // Обрабатываем штрихкод для поиска товара
                processBarcodeForProduct(
                    barcode = barcode,
                    onProductFound = { product ->
                        if (product != null) {
                            val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                TaskProduct(product = product)
                            } else {
                                product
                            }
                            context.onComplete(result)
                            context.onForward()
                        }
                    }
                )
            }
        }

        // Слушатель событий сканирования
        if (selectedInputMethod == InputMethod.HARDWARE_SCANNER) {
            ScannerListener(
                onBarcodeScanned = { barcode ->
                    processBarcodeForProduct(
                        barcode = barcode,
                        onProductFound = { product ->
                            if (product != null) {
                                val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                    TaskProduct(product = product)
                                } else {
                                    product
                                }
                                context.onComplete(result)
                                // Добавляем вызов onForward() для автоматического перехода к следующему шагу
                                context.onForward()
                                selectedInputMethod = InputMethod.NONE
                            }
                        }
                    )
                }
            )
        }

        // Загрузка продуктов при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            // Получаем IDs продуктов из плана
            val planProductIds = plannedProduct?.let { setOf(it.id) } ?: emptySet()
            wizardViewModel.loadProducts(searchQuery, planProductIds)
        }

        // Показываем диалог сканирования камерой, если выбран этот метод
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    processBarcodeForProduct(
                        barcode = barcode,
                        onProductFound = { product ->
                            if (product != null) {
                                val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                    TaskProduct(product = product)
                                } else {
                                    product
                                }
                                context.onComplete(result)
                                // Добавляем вызов onForward() для автоматического перехода к следующему шагу
                                context.onForward()
                            }
                            showCameraScannerDialog = false
                            selectedInputMethod = InputMethod.NONE
                        }
                    )
                },
                onClose = {
                    showCameraScannerDialog = false
                    selectedInputMethod = InputMethod.NONE
                },
                instructionText = if (plannedProduct != null)
                    stringResource(R.string.scan_product_expected, plannedProduct.name)
                else
                    stringResource(R.string.scan_product)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображаем выбранный продукт, если есть
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный продукт:",
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
                            .padding(8.dp)
                    ) {
                        Text(
                            text = selectedProduct.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Артикул: ${selectedProduct.articleNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Кнопка "Вперёд" для перехода к следующему шагу
                        if (context.hasStepResult) {
                            Button(
                                onClick = { context.onForward() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Вперёд")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Если не выбран способ ввода, показываем кнопки выбора метода
            if (!showScanMethodSelection && selectedInputMethod == InputMethod.NONE && selectedProduct == null) {
                Text(
                    text = stringResource(R.string.choose_scan_method),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Кнопки выбора метода ввода
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Кнопка для сканирования камерой
                    Button(
                        onClick = {
                            selectedInputMethod = InputMethod.CAMERA
                            showCameraScannerDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.scan_with_camera))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка для сканирования встроенным сканером (если доступен)
                    if (scannerService != null) {
                        Button(
                            onClick = { selectedInputMethod = InputMethod.HARDWARE_SCANNER },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(R.string.scan_with_scanner))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка для выбора из списка
                Button(
                    onClick = { showScanMethodSelection = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewList,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.select_from_list))
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Показываем интерфейс встроенного сканера, если выбран этот метод
            if (selectedInputMethod == InputMethod.HARDWARE_SCANNER) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (plannedProduct != null)
                                stringResource(R.string.scan_product_expected, plannedProduct.name)
                            else
                                stringResource(R.string.scan_product),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ожидание сканирования...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { selectedInputMethod = InputMethod.NONE },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }

            // Отображаем запланированные продукты, если они есть
            if (planProducts.isNotEmpty() && (showScanMethodSelection || selectedInputMethod == InputMethod.NONE)) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                ) {
                    items(planProducts) { taskProduct ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = taskProduct.product.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Артикул: ${taskProduct.product.articleNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (taskProduct.quantity > 0) {
                                    Text(
                                        text = "Количество: ${taskProduct.quantity}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        // Создаем результат в зависимости от типа объекта
                                        val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                            taskProduct
                                        } else {
                                            taskProduct.product
                                        }
                                        context.onComplete(result)
                                        showScanMethodSelection = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Поиск и список продуктов (показываются только если выбран режим ручного ввода)
            if (showScanMethodSelection) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск товара") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(products) { product ->
                        ProductItem(
                            product = product,
                            onClick = {
                                // Создаем результат в зависимости от типа объекта
                                val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                    TaskProduct(product = product)
                                } else {
                                    product
                                }

                                context.onComplete(result)
                                showScanMethodSelection = false
                            }
                        )
                    }
                }

                // Кнопка отмены для возврата к выбору способа ввода
                Button(
                    onClick = { showScanMethodSelection = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Отмена")
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Проверяем тип результата
        return when (step.objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> value is Product
            ActionObjectType.TASK_PRODUCT -> value is TaskProduct
            else -> false
        }
    }

    // Метод для обработки отсканированного штрихкода
    private fun processBarcodeForProduct(barcode: String, onProductFound: (Product?) -> Unit) {
        wizardViewModel.findProductByBarcode(barcode, onProductFound)
    }

    // Перечисление для методов ввода
    private enum class InputMethod {
        NONE,               // Режим выбора
        CAMERA,             // Сканирование камерой
        HARDWARE_SCANNER,   // Сканирование встроенным сканером
    }
}