package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.domain.entity.taskx.ObjectSelectionCondition
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.common.inputs.NumberTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView

// Шаг выбора товара
@Composable
fun ProductSelectionStep(
    promptText: String,
    selectionCondition: ObjectSelectionCondition,
    intermediateResults: Map<String, Any?>,
    onProductSelected: (TaskProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showScanner by remember { mutableStateOf(false) }

    // Здесь будет загрузка товаров в зависимости от selectionCondition
    LaunchedEffect(selectionCondition, searchQuery) {
        // TODO: загрузить товары через ViewModel/UseCase
        // Временные данные
        products = listOf(
            Product(
                id = "p1",
                name = "Наушники вкладыши",
                accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
                articleNumber = "H-12345",
                mainUnitId = "u1",
                units = emptyList()
            ),
            Product(
                id = "p2",
                name = "Молоко",
                accountingModel = com.synngate.synnframe.domain.entity.AccountingModel.QTY,
                articleNumber = "M-67890",
                mainUnitId = "u2",
                units = emptyList()
            )
        ).filter { product ->
            searchQuery.isEmpty() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.articleNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showScanner) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    // TODO: найти товар по штрихкоду
                    // Временное решение
                    if (products.isNotEmpty()) {
                        val selectedProduct = products.first()
                        // Создаем TaskProduct с выбранным товаром
                        val taskProduct = TaskProduct(
                            product = selectedProduct,
                            quantity = 1f, // Дефолтное количество
                            status = ProductStatus.STANDARD
                        )
                        onProductSelected(taskProduct)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Button(
                onClick = { showScanner = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Выбрать из списка")
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск товара") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showScanner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Сканировать штрихкод")
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onClick = {
                            // Создаем TaskProduct с выбранным товаром
                            val taskProduct = TaskProduct(
                                product = product,
                                quantity = 1f, // Дефолтное количество
                                status = ProductStatus.STANDARD
                            )
                            onProductSelected(taskProduct)
                        }
                    )
                }
            }
        }
    }
}

// Шаг ввода количества
@Composable
fun ProductQuantityStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onQuantityEntered: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableStateOf("1.0") }
    var isError by remember { mutableStateOf(false) }

    // Получаем текущий товар из промежуточных результатов
    val taskProduct = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        taskProduct?.let { product ->
            Text(
                text = "Товар: ${product.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Артикул: ${product.product.articleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        NumberTextField(
            value = quantity,
            onValueChange = {
                quantity = it
                isError = false
            },
            label = "Количество",
            isError = isError,
            errorText = if (isError) "Введите корректное значение" else null,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                try {
                    val numberValue = quantity.toFloat()
                    if (numberValue > 0) {
                        onQuantityEntered(numberValue)
                    } else {
                        isError = true
                    }
                } catch (e: Exception) {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Подтвердить")
        }
    }
}

// Шаг выбора ячейки
@Composable
fun BinSelectionStep(
    promptText: String,
    zoneFilter: String?,
    onBinSelected: (BinX) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var bins by remember { mutableStateOf<List<BinX>>(emptyList()) }
    var showScanner by remember { mutableStateOf(false) }

    // Здесь будет загрузка ячеек в зависимости от зоны
    LaunchedEffect(zoneFilter, searchQuery) {
        // TODO: загрузить ячейки через ViewModel/UseCase
        // Временные данные
        bins = listOf(
            BinX(
                code = "A00111",
                zone = "Приемка",
                line = "A",
                rack = "01",
                tier = "1",
                position = "1"
            ),
            BinX(
                code = "A00112",
                zone = "Приемка",
                line = "A",
                rack = "01",
                tier = "1",
                position = "2"
            ),
            BinX(
                code = "B00211",
                zone = "Хранение",
                line = "B",
                rack = "02",
                tier = "1",
                position = "1"
            )
        ).filter { bin ->
            (zoneFilter == null || bin.zone == zoneFilter) &&
                    (searchQuery.isEmpty() || bin.code.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showScanner) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    // TODO: найти ячейку по штрихкоду
                    // Временное решение
                    bins.find { it.code == barcode }?.let { bin ->
                        onBinSelected(bin)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Button(
                onClick = { showScanner = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Выбрать из списка")
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск ячейки") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showScanner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Сканировать штрихкод")
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(bins) { bin ->
                    BinItem(
                        bin = bin,
                        onClick = { onBinSelected(bin) }
                    )
                }
            }
        }
    }
}

// Шаг выбора паллеты
@Composable
fun PalletSelectionStep(
    promptText: String,
    selectionCondition: ObjectSelectionCondition,
    onPalletSelected: (Pallet) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var pallets by remember { mutableStateOf<List<Pallet>>(emptyList()) }
    var showScanner by remember { mutableStateOf(false) }

    // Здесь будет загрузка паллет
    LaunchedEffect(selectionCondition, searchQuery) {
        // TODO: загрузить паллеты через ViewModel/UseCase
        // Временные данные
        pallets = listOf(
            Pallet(code = "IN000000001", isClosed = true),
            Pallet(code = "IN000000002", isClosed = false),
            Pallet(code = "IN000000003", isClosed = false)
        ).filter { pallet ->
            searchQuery.isEmpty() || pallet.code.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showScanner) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    // TODO: найти паллету по штрихкоду
                    // Временное решение
                    pallets.find { it.code == barcode }?.let { pallet ->
                        onPalletSelected(pallet)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Button(
                onClick = { showScanner = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Выбрать из списка")
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск паллеты") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showScanner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Сканировать штрихкод")
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(pallets) { pallet ->
                    PalletItem(
                        pallet = pallet,
                        onClick = { onPalletSelected(pallet) }
                    )
                }
            }
        }
    }
}

// Шаг создания паллеты
@Composable
fun CreatePalletStep(
    promptText: String,
    onPalletCreated: (Pallet) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreating by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Нажмите кнопку для создания новой паллеты",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                isCreating = true
                // TODO: Создать паллету через ViewModel/UseCase
                // Временное решение
                val newPallet = Pallet(
                    code = "IN000000${(1000..9999).random()}",
                    isClosed = false
                )
                onPalletCreated(newPallet)
            },
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCreating) "Создание..." else "Создать паллету")
        }
    }
}

// Шаг закрытия паллеты
@Composable
fun ClosePalletStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onPalletClosed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isClosing by remember { mutableStateOf(false) }

    // Получаем текущую паллету из промежуточных результатов
    val pallet = intermediateResults["PLACEMENT_PALLET"] as? Pallet

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        pallet?.let {
            Text(
                text = "Паллета: ${it.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Статус: ${if (it.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                isClosing = true
                // TODO: Закрыть паллету через ViewModel/UseCase
                onPalletClosed(true)
            },
            enabled = !isClosing && pallet != null && !pallet.isClosed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isClosing) "Закрытие..." else "Закрыть паллету")
        }
    }
}

// Шаг печати этикетки
@Composable
fun PrintLabelStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onLabelPrinted: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPrinting by remember { mutableStateOf(false) }

    // Получаем текущую паллету или товар из промежуточных результатов
    val pallet = intermediateResults["PLACEMENT_PALLET"] as? Pallet
    val product = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        pallet?.let {
            Text(
                text = "Печать этикетки для паллеты: ${it.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        product?.let {
            Text(
                text = "Печать этикетки для товара: ${it.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                isPrinting = true
                // TODO: Отправить на печать через ViewModel/UseCase
                onLabelPrinted(true)
            },
            enabled = !isPrinting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isPrinting) "Печать..." else "Напечатать этикетку")
        }
    }
}

// Итоговый шаг - показывает сводку
@Composable
fun SummaryStep(
    state: FactLineWizardState,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val intermediateResults = state.getIntermediateResults()

        // Отображаем товар
        intermediateResults["STORAGE_PRODUCT"]?.let { product ->
            product as TaskProduct
            Text(
                text = "Товар: ${product.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Артикул: ${product.product.articleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Количество: ${product.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (product.hasExpirationDate()) {
                Text(
                    text = "Срок годности: ${product.expirationDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = "Статус: ${product.status}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображаем паллету размещения
        intermediateResults["PLACEMENT_PALLET"]?.let { pallet ->
            pallet as Pallet
            Text(
                text = "Паллета размещения: ${pallet.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображаем ячейку размещения
        intermediateResults["PLACEMENT_BIN"]?.let { bin ->
            bin as BinX
            Text(
                text = "Ячейка размещения: ${bin.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Кнопки действий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Подтвердить")
            }
        }
    }
}

// Вспомогательные компоненты элементов списка

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Артикул: ${product.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BinItem(
    bin: BinX,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${bin.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PalletItem(
    pallet: Pallet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${pallet.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}