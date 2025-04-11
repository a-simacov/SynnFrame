package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProductSelectionStep(
    promptText: String,
    selectionCondition: ObjectSelectionCondition,
    intermediateResults: Map<String, Any?>,
    onProductSelected: (TaskProduct) -> Unit,
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Получаем продукты через ViewModel
    val products by viewModel.products.collectAsState()

    // Загрузка продуктов при изменении поискового запроса
    LaunchedEffect(selectionCondition, searchQuery) {
        // Получаем IDs продуктов из плана, если нужно
        val planProductIds = if (selectionCondition == ObjectSelectionCondition.FROM_PLAN) {
            intermediateResults["PLAN_PRODUCT_IDS"] as? Set<String>
        } else {
            null
        }

        viewModel.loadProducts(searchQuery, planProductIds)
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
                    viewModel.findProductByBarcode(barcode) { product ->
                        if (product != null) {
                            // Создаем TaskProduct из найденного продукта
                            val taskProduct = TaskProduct(
                                product = product,
                                quantity = 1f,
                                status = ProductStatus.STANDARD
                            )
                            onProductSelected(taskProduct)
                        }
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
                            val taskProduct = TaskProduct(
                                product = product,
                                quantity = 1f,
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Получение данных из ViewModel
    val bins by viewModel.bins.collectAsState()

    // Загрузка ячеек при изменении поискового запроса
    LaunchedEffect(zoneFilter, searchQuery) {
        viewModel.loadBins(searchQuery, zoneFilter)
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
                    viewModel.findBinByCode(barcode) { bin ->
                        if (bin != null) {
                            onBinSelected(bin)
                        }
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Получение данных из ViewModel
    val pallets by viewModel.pallets.collectAsState()

    // Загрузка паллет при изменении поискового запроса
    LaunchedEffect(selectionCondition, searchQuery) {
        viewModel.loadPallets(searchQuery)
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
                    viewModel.findPalletByCode(barcode) { pallet ->
                        if (pallet != null) {
                            onPalletSelected(pallet)
                        }
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
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
                viewModel.createPallet { result ->
                    isCreating = false
                    result.getOrNull()?.let { newPallet ->
                        onPalletCreated(newPallet)
                    }
                }
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
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
                pallet?.let { currentPallet ->
                    viewModel.closePallet(currentPallet.code) { result ->
                        isClosing = false
                        onPalletClosed(result.isSuccess)
                    }
                }
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
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
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
                pallet?.let { currentPallet ->
                    viewModel.printPalletLabel(currentPallet.code) { result ->
                        isPrinting = false
                        onLabelPrinted(result.isSuccess)
                    }
                } ?: run {
                    // Если нет паллеты, просто имитируем успешную печать для этикетки товара
                    isPrinting = false
                    onLabelPrinted(true)
                }
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

@Composable
fun ExpirationDateStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onDateEntered: (LocalDate) -> Unit,
    viewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    val storageProduct = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) } // По умолчанию +30 дней

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Показать информацию о выбранном товаре
        storageProduct?.let { product ->
            Text(
                text = "Товар: ${product.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Компонент выбора даты (упрощенный вариант)
        DatePickerComponent(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            // Кнопка "Пропустить"
            OutlinedButton(
                onClick = { onDateEntered(LocalDate.of(1970, 1, 1)) }, // Дата-заглушка
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Пропустить")
            }

            // Кнопка "Подтвердить"
            Button(
                onClick = { onDateEntered(selectedDate) }
            ) {
                Text("Подтвердить")
            }
        }
    }
}

@Composable
fun DatePickerComponent(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
    modifier: Modifier = Modifier
) {
    // Упрощенный компонент для выбора даты
    // В реальном приложении здесь был бы полноценный DatePicker
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Выбранная дата: ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
    }
}

@Composable
fun ProductStatusStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onStatusSelected: (ProductStatus) -> Unit,
    viewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    val storageProduct = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct
    var selectedStatus by remember { mutableStateOf(ProductStatus.STANDARD) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Показать информацию о выбранном товаре
        storageProduct?.let { product ->
            Text(
                text = "Товар: ${product.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Радио-кнопки для выбора статуса
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductStatus.entries.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedStatus = status },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            ProductStatus.STANDARD -> "Кондиция (стандарт)"
                            ProductStatus.DEFECTIVE -> "Брак"
                            ProductStatus.EXPIRED -> "Просрочен"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка подтверждения
        Button(
            onClick = { onStatusSelected(selectedStatus) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Подтвердить")
        }
    }
}