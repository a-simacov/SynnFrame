package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.ProductItem
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора продукта с улучшенным интерфейсом
 */
class ProductSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Состояния поля поиска и ввода
        var manualProductCode by remember { mutableStateOf("") }
        var showProductList by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        // Состояние для сообщений об ошибках
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Состояние для диалога сканирования камерой
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        // Для отображения сообщений
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Получение данных о продуктах из ViewModel
        val products by wizardViewModel.products.collectAsState()

        // Получаем запланированный продукт
        val plannedProduct = action.storageProduct?.product

        // Список запланированных продуктов для отображения
        val planProducts = remember(action) {
            listOfNotNull(action.storageProduct)
        }

        // Получаем уже выбранный продукт из контекста, если есть
        val selectedProduct = remember(context.results) {
            context.results[step.id] as? Product ?:
            (context.results[step.id] as? TaskProduct)?.product
        }

        // Получаем сервис сканера для встроенного сканера
        val scannerService = LocalScannerService.current

        // Функция для показа сообщения об ошибке
        val showError = { message: String ->
            errorMessage = message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        // Функция поиска продукта по штрихкоду или артикулу
        val searchProduct = { barcode: String ->
            if (barcode.isNotEmpty()) {
                Timber.d("Поиск продукта по штрихкоду: $barcode")
                errorMessage = null // Сбрасываем предыдущую ошибку

                processBarcodeForProduct(
                    barcode = barcode,
                    action = action,
                    onProductFound = { product ->
                        if (product != null) {
                            Timber.d("Продукт найден: ${product.name}")

                            // Создаем результат в зависимости от типа объекта
                            val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                TaskProduct(product = product)
                            } else {
                                product
                            }

                            // Вызываем onComplete для передачи результата
                            context.onComplete(result)
                        } else {
                            Timber.w("Продукт не найден: $barcode")
                            showError("Продукт со штрихкодом '$barcode' не найден")
                        }
                    }
                )

                // Очищаем поле ввода после поиска
                manualProductCode = ""
            }
        }

        // Использование BarcodeHandlerWithState для обработки штрихкодов
        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от сканера: $barcode")
                errorMessage = null // Сбрасываем предыдущую ошибку

                processBarcodeForProduct(
                    barcode = barcode,
                    action = action,
                    onProductFound = { product ->
                        if (product != null) {
                            Timber.d("Продукт найден: ${product.name}")

                            // Создаем результат в зависимости от типа объекта
                            val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                TaskProduct(product = product)
                            } else {
                                product
                            }

                            // Вызываем onComplete для передачи результата
                            context.onComplete(result)
                        } else {
                            Timber.w("Продукт не найден: $barcode")
                            showError("Продукт со штрихкодом '$barcode' не найден")
                            // Сбрасываем состояние обработки, чтобы можно было повторить сканирование
                            setProcessingState(false)
                        }
                    }
                )

                // Очищаем поле ввода после поиска
                manualProductCode = ""
            }
        )

        // Обработка внешнего штрихкода из контекста
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")

                // Просто перенаправляем в функцию поиска продукта
                searchProduct(barcode)
            }
        }

        // Загрузка продуктов при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            // Получаем IDs продуктов из плана
            val planProductIds = plannedProduct?.let { setOf(it.id) } ?: emptySet()
            wizardViewModel.loadProducts(searchQuery, planProductIds)
        }

        // Показываем диалог сканирования камерой, если он активирован
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    Timber.d("Получен штрихкод от камеры: $barcode")
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

        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок с описанием действия WMS
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Поле для ручного ввода штрихкода продукта
            OutlinedTextField(
                value = manualProductCode,
                onValueChange = {
                    manualProductCode = it
                    errorMessage = null // Сбрасываем ошибку при вводе
                },
                label = { Text(stringResource(R.string.enter_product_barcode)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchProduct(manualProductCode) }),
                trailingIcon = {
                    IconButton(onClick = { showCameraScannerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_with_camera)
                        )
                    }
                },
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Ошибка",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные продукты, если они есть
            if (planProducts.isNotEmpty()) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
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
                                }

                                IconButton(
                                    onClick = {
                                        // Создаем результат в зависимости от типа объекта
                                        val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                            taskProduct
                                        } else {
                                            taskProduct.product
                                        }
                                        context.onComplete(result)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбрать",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Кнопка для выбора из списка (если нет запланированного продукта или есть, но можно выбрать любой)
            if (plannedProduct == null || !step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
                Button(
                    onClick = {
                        showProductList = !showProductList
                        // Загружаем продукты при открытии списка
                        if (showProductList) {
                            wizardViewModel.loadProducts("", plannedProduct?.let { setOf(it.id) })
                        }
                    },
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
                    Text(if (showProductList) "Скрыть список" else "Выбрать из списка")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Список продуктов для выбора (показывается только если активирован)
            if (showProductList) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_product)) },
                    placeholder = { Text(stringResource(R.string.search_product_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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

    // Изменяем метод обработки штрихкода для товара, чтобы не обращаться к репозиторию
    private fun processBarcodeForProduct(
        barcode: String,
        action: PlannedAction,
        onProductFound: (Product?) -> Unit
    ) {
        // Получаем запланированный товар из контекста
        val plannedProduct = action.storageProduct?.product

        // Если есть запланированный товар, проверяем соответствие
        if (plannedProduct != null) {
            // В реальном приложении здесь может быть более сложная логика сравнения
            // Например, проверка соответствия штрихкода одному из штрихкодов товара
            // Но пока просто возвращаем запланированный товар
            Timber.d("Найден товар из плана: ${plannedProduct.name}")
            onProductFound(plannedProduct)
        } else {
            // Если запланированного товара нет, создаем временный объект товара
            // без обращения к репозиторию
            val tempProduct = Product(
                id = "temp_${System.currentTimeMillis()}",
                name = "Товар со штрихкодом $barcode",
                accountingModel = AccountingModel.QTY,
                articleNumber = barcode,
                mainUnitId = "unknown",
                units = emptyList()
            )

            Timber.d("Создан временный товар для штрихкода: $barcode")
            onProductFound(tempProduct)
        }
    }
}