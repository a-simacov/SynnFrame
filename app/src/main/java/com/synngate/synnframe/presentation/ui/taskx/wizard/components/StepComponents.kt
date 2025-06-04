package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun StorageProductStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedProduct = state.plannedAction?.storageProduct
    val selectedProduct = state.selectedObjects[step.id] as? TaskProduct

    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showProductSelector by remember { mutableStateOf(false) }

    // Проверяем наличие serverSelectionEndpoint
    val useServerRequest = step.serverSelectionEndpoint.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLocked) {
            selectedProduct?.let { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Выбранный товар (заблокирован):",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = product.product.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Артикул: ${product.product.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (product.hasExpirationDate()) {
                            Text(
                                text = "Срок годности: ${product.expirationDate?.toLocalDate()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "Статус: ${product.status.format()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Если настроен серверный запрос, показываем кнопку для получения объекта
            if (useServerRequest) {
                LoadingButton(
                    onClick = onRequestServerObject,
                    isLoading = state.isRequestingServerObject,
                    onCancel = onCancelServerRequest,
                    text = "Получить товар задания с сервера",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            WizardBarcodeField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                onSearch = {
                    if (barcodeValue.isNotEmpty()) {
                        Timber.d("Поиск по введенному штрихкоду: $barcodeValue")
                        handleBarcode(barcodeValue)
                    }
                },
                onScannerClick = { showScanner = true },
                onSelectFromList = { showProductSelector = true },
                label = "Штрихкод, ID, артикул",
                placeholder = "Введите или отсканируйте",
                enabled = !useServerRequest && !state.isRequestingServerObject // Блокируем поле, если используется серверный запрос
            )

            if (selectedProduct != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Выбранный товар:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = selectedProduct.product.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Артикул: ${selectedProduct.product.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selectedProduct.hasExpirationDate()) {
                            Text(
                                text = "Срок годности: ${selectedProduct.expirationDate?.toLocalDate()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "Статус: ${selectedProduct.status.format()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (state.shouldShowAdditionalProps(step)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    AdditionalPropsProductForm(
                        state = state,
                        step = step,
                        onObjectSelected = onObjectSelected,
                        isLocked = isLocked
                    )
                }
            } else if (plannedProduct != null) {
                Spacer(modifier = Modifier.height(8.dp))

                StorageProductCard(
                    product = plannedProduct,
                    isSelected = false,
                    onSelect = { onObjectSelected(plannedProduct) },
                    isPlanned = true
                )

                if (state.shouldShowAdditionalProps(step)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    AdditionalPropsProductForm(
                        state = state,
                        step = step,
                        onObjectSelected = onObjectSelected,
                        isLocked = isLocked
                    )
                }
            } else if (state.shouldShowAdditionalProps(step)) {
                Spacer(modifier = Modifier.height(8.dp))

                AdditionalPropsProductForm(
                    state = state,
                    step = step,
                    onObjectSelected = onObjectSelected,
                    isLocked = isLocked
                )
            }
        }
    }

    if (showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                barcodeValue = barcode
                showScanner = false
                handleBarcode(barcode)
            },
            onClose = { showScanner = false },
            instructionText = "Отсканируйте штрихкод товара"
        )
    }

    if (showProductSelector) {
        OptimizedProductSelectionDialog(
            onProductSelected = { product ->
                val taskProduct = if (state.shouldShowAdditionalProps(step)) {
                    val baseTaskProduct = state.getTaskProductFromClassifier(step.id)
                    baseTaskProduct.copy(
                        product = product
                    )
                } else {
                    TaskProduct(
                        id = java.util.UUID.randomUUID().toString(),
                        product = product,
                        status = com.synngate.synnframe.domain.entity.taskx.ProductStatus.STANDARD
                    )
                }
                onObjectSelected(taskProduct)
            },
            onDismiss = { showProductSelector = false },
            initialFilter = barcodeValue,
            planProductIds = if (plannedProduct != null) {
                setOf(plannedProduct.product.id)
            } else state.plannedAction?.storageProductClassifier?.let {
                setOf(it.id)
            }
        )
    }
}

@Composable
fun StorageProductCard(
    product: TaskProduct,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isPlanned: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")

    PlannedObjectCard(
        title = product.product.name,
        subtitle = buildString {
            append("Артикул: ${product.product.articleNumber}")
            if (product.hasExpirationDate()) {
                append(
                    "\nСрок годности: ${
                        product.expirationDate?.toLocalDate()?.format(dateFormatter) ?: "Не указан"
                    }"
                )
            }
            append("\nСтатус: ${product.status.format()}")
        },
        isSelected = isSelected,
        onClick = onSelect,
        isPlanned = isPlanned
    )
}

@Composable
fun AdditionalPropsProductForm(
    state: ActionWizardState,
    step: ActionStepTemplate,
    onObjectSelected: (Any) -> Unit,
    isLocked: Boolean = false
) {
    val classifierProduct = state.plannedAction?.storageProductClassifier
    val taskProduct = state.getTaskProductFromClassifier(step.id)
    val isSelected = state.selectedObjects[step.id] != null

    var hasExpirationDate by remember(taskProduct) { mutableStateOf(taskProduct.expirationDate != null) }
    var hasStatusSelected by remember(taskProduct) { mutableStateOf(true) }

    val needsExpirationDate = state.shouldShowExpirationDate()

    if (isLocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = classifierProduct?.name ?: "Товар",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Артикул: ${classifierProduct?.articleNumber ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (taskProduct.hasExpirationDate()) {
                    Text(
                        text = "Срок годности: ${taskProduct.expirationDate?.toLocalDate()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Статус: ${taskProduct.status.format()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (isSelected) {
        Spacer(modifier = Modifier.height(8.dp))

        if (state.shouldShowExpirationDate()) {
            ExpirationDatePicker(
                expirationDate = taskProduct.expirationDate,
                onDateSelected = { date ->
                    hasExpirationDate = date != null
                    val updatedProduct = taskProduct.copy(expirationDate = date)
                    onObjectSelected(updatedProduct)
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ProductStatusSelector(
            selectedStatus = taskProduct.status,
            onStatusSelected = { status ->
                hasStatusSelected = true
                val updatedProduct = taskProduct.copy(status = status)
                onObjectSelected(updatedProduct)
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (!state.shouldShowExpirationDate() && state.isLoadingProductInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Загрузка информации о товаре...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        if (classifierProduct != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {

                    Text(
                        text = classifierProduct.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Артикул: ${classifierProduct.articleNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ID: ${classifierProduct.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProductClassifierStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedProduct = state.plannedAction?.storageProductClassifier
    val selectedProduct = state.selectedObjects[step.id] as? Product

    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showProductSelector by remember { mutableStateOf(false) }

    // Проверяем наличие serverSelectionEndpoint
    val useServerRequest = step.serverSelectionEndpoint.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLocked) {
            selectedProduct?.let { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    ProductClassifierCard(
                        product = product,
                        isSelected = true,
                        onClick = { /* Ничего не делаем, поле заблокировано */ },
                        isLocked = true,
                        icon = Icons.Default.Lock
                    )
                }
            }
        } else {
            // Если настроен серверный запрос, показываем кнопку для получения объекта
            if (useServerRequest) {
                LoadingButton(
                    onClick = onRequestServerObject,
                    isLoading = state.isRequestingServerObject,
                    onCancel = onCancelServerRequest,
                    text = "Получить товар с сервера",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            WizardBarcodeField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                onSearch = {
                    if (barcodeValue.isNotEmpty()) {
                        Timber.d("Поиск по введенному штрихкоду: $barcodeValue")
                        handleBarcode(barcodeValue)
                    }
                },
                onScannerClick = { showScanner = true },
                onSelectFromList = { showProductSelector = true },
                label = "Штрихкод, ID, артикул",
                placeholder = "Введите или отсканируйте",
                enabled = !useServerRequest && !state.isRequestingServerObject // Блокируем поле, если используется серверный запрос
            )

            if (selectedProduct != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    ProductClassifierCard(
                        product = selectedProduct,
                        isSelected = true,
                        onClick = { /* Уже выбрано */ }
                    )
                }
            } else if (plannedProduct != null) {
                Spacer(modifier = Modifier.height(8.dp))

                ProductClassifierCard(
                    product = plannedProduct,
                    isSelected = false,
                    onClick = { onObjectSelected(plannedProduct) },
                    isPlanned = true
                )
            }
        }
    }

    if (showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                barcodeValue = barcode
                showScanner = false
                handleBarcode(barcode)
            },
            onClose = { showScanner = false },
            instructionText = "Отсканируйте штрихкод товара"
        )
    }

    if (showProductSelector) {
        OptimizedProductSelectionDialog(
            onProductSelected = { product ->
                onObjectSelected(product)
            },
            onDismiss = { showProductSelector = false },
            initialFilter = barcodeValue,
            planProductIds = state.plannedAction?.storageProductClassifier?.let { setOf(it.id) }
        )
    }
}

@Composable
fun ProductClassifierCard(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLocked: Boolean = false,
    isPlanned: Boolean = false,
    icon: ImageVector? = null
) {
    PlannedObjectCard(
        title = product.name,
        subtitle = "Артикул: ${product.articleNumber}\nID: ${product.id}",
        isSelected = isSelected,
        onClick = onClick,
        isLocked = isLocked,
        isPlanned = isPlanned,
        icon = icon
    )
}

@Composable
fun BinStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    isStorage: Boolean,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedBin = if (isStorage)
        state.plannedAction?.storageBin
    else
        state.plannedAction?.placementBin

    val selectedBin = state.selectedObjects[step.id] as? BinX
    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Проверяем наличие serverSelectionEndpoint
    val useServerRequest = step.serverSelectionEndpoint.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLocked) {
            selectedBin?.let { bin ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    PlannedObjectCard(
                        title = "Ячейка: ${bin.code}",
                        subtitle = bin.zone.takeIf { it.isNotEmpty() }?.let { "Зона: $it" },
                        isSelected = true,
                        onClick = { /* Ничего не делаем, поле заблокировано */ },
                        isLocked = true,
                        icon = Icons.Default.Lock
                    )
                }
            }
        } else {
            // Если настроен серверный запрос, показываем кнопку для получения объекта
            if (useServerRequest) {
                LoadingButton(
                    onClick = onRequestServerObject,
                    isLoading = state.isRequestingServerObject,
                    onCancel = onCancelServerRequest,
                    text = "Получить ${if (isStorage) "ячейку хранения" else "ячейку размещения"} с сервера",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            WizardBarcodeField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                onSearch = {
                    if (barcodeValue.isNotEmpty()) {
                        Timber.d("Поиск ячейки по коду: $barcodeValue")
                        handleBarcode(barcodeValue)
                    }
                },
                onScannerClick = { showScanner = true },
                label = if (isStorage) "Код ячейки хранения" else "Код ячейки размещения",
                placeholder = "Код ячейки",
                enabled = !useServerRequest && !state.isRequestingServerObject // Блокируем поле, если используется серверный запрос
            )

            if (selectedBin != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    PlannedObjectCard(
                        title = "Ячейка: ${selectedBin.code}",
                        subtitle = selectedBin.zone.takeIf { it.isNotEmpty() }?.let { "Зона: $it" },
                        isSelected = true,
                        onClick = { /* Уже выбрано */ }
                    )
                }
            }
            // Показываем запланированную ячейку только если нет выбранной
            else if (plannedBin != null) {
                Spacer(modifier = Modifier.height(8.dp))

                PlannedObjectCard(
                    title = "Ячейка: ${plannedBin.code}",
                    subtitle = "Зона: ${plannedBin.zone}",
                    isSelected = false,
                    onClick = { onObjectSelected(plannedBin) },
                    isPlanned = true
                )
            }
        }
    }

    if (showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                barcodeValue = barcode
                showScanner = false
                handleBarcode(barcode)
            },
            onClose = { showScanner = false },
            instructionText = if (isStorage) "Отсканируйте код ячейки хранения" else "Отсканируйте код ячейки размещения"
        )
    }
}

@Composable
fun PalletStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    isStorage: Boolean,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedPallet = if (isStorage)
        state.plannedAction?.storagePallet
    else
        state.plannedAction?.placementPallet

    val selectedPallet = state.selectedObjects[step.id] as? Pallet
    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    // Проверяем наличие serverSelectionEndpoint
    val useServerRequest = step.serverSelectionEndpoint.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLocked) {
            selectedPallet?.let { pallet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    PlannedObjectCard(
                        title = "Паллета: ${pallet.code}",
                        subtitle = if (pallet.isClosed) "Закрыта" else "Открыта",
                        isSelected = true,
                        onClick = { /* Ничего не делаем, поле заблокировано */ },
                        isLocked = true,
                        icon = Icons.Default.Lock
                    )
                }
            }
        } else {
            // Если настроен серверный запрос, показываем кнопку для получения объекта
            if (useServerRequest) {
                LoadingButton(
                    onClick = onRequestServerObject,
                    isLoading = state.isRequestingServerObject,
                    onCancel = onCancelServerRequest,
                    text = "Получить ${if (isStorage) "паллету хранения" else "паллету размещения"} с сервера",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            WizardBarcodeField(
                value = barcodeValue,
                onValueChange = { barcodeValue = it },
                onSearch = {
                    if (barcodeValue.isNotEmpty()) {
                        Timber.d("Поиск паллеты по коду: $barcodeValue")
                        handleBarcode(barcodeValue)
                    }
                },
                onScannerClick = { showScanner = true },
                label = if (isStorage) "Код паллеты хранения" else "Код паллеты размещения",
                placeholder = "Код паллеты",
                enabled = !useServerRequest && !state.isRequestingServerObject // Блокируем поле, если используется серверный запрос
            )

            if (selectedPallet != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    PlannedObjectCard(
                        title = "Паллета: ${selectedPallet.code}",
                        subtitle = if (selectedPallet.isClosed) "Закрыта" else "Открыта",
                        isSelected = true,
                        onClick = { /* Уже выбрано */ }
                    )
                }
            } else if (plannedPallet != null) {
                Spacer(modifier = Modifier.height(8.dp))

                PlannedObjectCard(
                    title = "Паллета: ${plannedPallet.code}",
                    subtitle = if (plannedPallet.isClosed) "Закрыта" else "Открыта",
                    isSelected = false,
                    onClick = { onObjectSelected(plannedPallet) },
                    isPlanned = true
                )
            }
        }
    }

    if (showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                barcodeValue = barcode
                showScanner = false
                handleBarcode(barcode)
            },
            onClose = { showScanner = false },
            instructionText = if (isStorage) "Отсканируйте код паллеты хранения" else "Отсканируйте код паллеты размещения"
        )
    }
}

@Composable
fun QuantityStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onQuantityChanged: (Float, Boolean) -> Unit,
    onRequestServerObject: () -> Unit,
    onCancelServerRequest: () -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedQuantity = state.plannedAction?.quantity ?: 0f
    val selectedQuantity = (state.selectedObjects[step.id] as? Number)?.toFloat() ?: 0f

    // Для упрощения считаем, что в контексте этого шага ещё не было выполненных действий
    // В реальном сценарии тут должна быть более сложная логика получения completedQuantity
    val completedQuantity = 0f

    val projectedTotalQuantity = completedQuantity + selectedQuantity
    val remainingAfterInput = (plannedQuantity - projectedTotalQuantity).coerceAtLeast(0f)
    val willExceedPlan = projectedTotalQuantity > plannedQuantity

    // Проверяем наличие serverSelectionEndpoint
    val useServerRequest = step.serverSelectionEndpoint.isNotEmpty()

    val focusRequester = remember { FocusRequester() }
    var inputValue by remember {
        mutableStateOf(if (selectedQuantity > 0) selectedQuantity.toString() else "")
    }

    // Устанавливаем фокус на поле ввода при первом отображении, если не заблокировано
    LaunchedEffect(Unit) {
        if (!isLocked && !useServerRequest) {
            try {
                delay(100)
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при установке фокуса на поле ввода количества")
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (plannedQuantity > 0) {
            QuantityIndicators(
                plannedQuantity = plannedQuantity,
                completedQuantity = completedQuantity,
                projectedTotalQuantity = projectedTotalQuantity,
                remainingAfterInput = remainingAfterInput,
                willExceedPlan = willExceedPlan
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (isLocked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Количество (заблокировано):",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = formatQuantityDisplay(selectedQuantity),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Заблокировано",
                            tint = Color(0xFFEC407A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Значение заблокировано",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEC407A)
                        )
                    }
                }
            }
        } else {
            // Если настроен серверный запрос, показываем кнопку для получения объекта
            if (useServerRequest) {
                LoadingButton(
                    onClick = onRequestServerObject,
                    isLoading = state.isRequestingServerObject,
                    onCancel = onCancelServerRequest,
                    text = "Получить количество с сервера",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            WizardQuantityInput(
                value = inputValue,
                onValueChange = { newValue ->
                    inputValue = newValue
                    // Передаем значение в ViewModel БЕЗ автоперехода
                    newValue.toFloatOrNull()?.let { onQuantityChanged(it, false) }
                },
                onIncrement = {
                    val newValue = (inputValue.toFloatOrNull() ?: 0f) + 1f
                    inputValue = newValue.toString()
                    // Передаем значение в ViewModel БЕЗ автоперехода
                    onQuantityChanged(newValue, false)
                },
                onDecrement = {
                    val currentValue = inputValue.toFloatOrNull() ?: 0f
                    if (currentValue > 1) {
                        val newValue = currentValue - 1f
                        inputValue = newValue.toString()
                        // Передаем значение в ViewModel БЕЗ автоперехода
                        onQuantityChanged(newValue, false)
                    }
                },
                onImeAction = {
                    // Только при нажатии на кнопку Done делаем автопереход
                    if (inputValue.isNotEmpty()) {
                        inputValue.toFloatOrNull()?.let {
                            Timber.d("IME-действие: количество $it")
                            // Передаем значение в ViewModel С автопереходом
                            onQuantityChanged(it, true)
                        }
                    }
                },
                isError = state.error != null,
                errorText = state.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                label = step.promptText,
                focusRequester = focusRequester,
                enabled = !useServerRequest && !state.isRequestingServerObject // Блокируем поле, если используется серверный запрос
            )
        }

        if (willExceedPlan && plannedQuantity > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            WarningMessage(message = "Внимание: превышение планового количества!")
        }
    }
}

@Composable
private fun QuantityIndicators(
    plannedQuantity: Float,
    completedQuantity: Float,
    projectedTotalQuantity: Float,
    remainingAfterInput: Float,
    willExceedPlan: Boolean
) {
    val color = when {
        willExceedPlan -> MaterialTheme.colorScheme.error
        plannedQuantity == projectedTotalQuantity -> Color(0xFF4CAF50) // Green
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        QuantityColumn(
            label = "план",
            valueLarge = formatQuantityDisplay(plannedQuantity),
            valueSmall = formatQuantityDisplay(completedQuantity),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(32.dp))
        QuantityColumn(
            label = "будет",
            valueLarge = formatQuantityDisplay(projectedTotalQuantity),
            valueSmall = formatQuantityDisplay(remainingAfterInput),
            color = color
        )
    }
}

@Composable
fun WarningMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun PlannedObjectCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLocked: Boolean = false,
    isPlanned: Boolean = false, // Новый параметр
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (!isLocked) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                if (isPlanned) {
                    Text(
                        text = "Запланировано:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isLocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon ?: Icons.Default.Lock,
                            contentDescription = "Заблокировано",
                            tint = Color(0xFFEC407A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Значение заблокировано",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEC407A)
                        )
                    }
                }
            }
        }
    }
}