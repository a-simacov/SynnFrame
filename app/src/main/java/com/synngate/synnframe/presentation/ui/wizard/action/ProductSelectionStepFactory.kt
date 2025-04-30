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
import androidx.compose.material3.Button
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
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора продукта
 * Упрощенная версия, работающая только с локальными данными
 */
class ProductSelectionStepFactory : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Состояния UI
        var manualProductCode by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        // Вспомогательные состояния и объекты
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Получаем запланированный продукт из действия
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

        // Функция поиска продукта по штрихкоду
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
                searchProduct(barcode)
            }
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

            // Кнопка с уведомлением, что работаем только с планируемыми товарами
            Button(
                onClick = { /* Только уведомление */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Работаем только с планируемыми товарами")
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

    // Упрощенный метод обработки штрихкода для товара, работающий только с запланированными товарами
    private fun processBarcodeForProduct(
        barcode: String,
        action: PlannedAction,
        onProductFound: (Product?) -> Unit
    ) {
        // Получаем запланированный товар из контекста
        val plannedProduct = action.storageProduct?.product

        // Если есть запланированный товар, считаем что штрихкод совпадает и возвращаем запланированный товар
        if (plannedProduct != null) {
            Timber.d("Найден товар из плана: ${plannedProduct.name}")
            onProductFound(plannedProduct)
        } else {
            // Создаем временный товар для демонстрации
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