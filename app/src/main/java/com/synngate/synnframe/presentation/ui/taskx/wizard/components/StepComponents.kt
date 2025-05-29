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
import androidx.compose.material3.OutlinedTextField
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
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedProduct = state.plannedAction?.storageProduct
    val selectedProduct = state.selectedObjects[step.id] as? TaskProduct

    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showProductSelector by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (plannedProduct != null) {
            StorageProductCard(
                product = plannedProduct,
                isSelected = selectedProduct != null,
                onSelect = { onObjectSelected(plannedProduct) },
                isLocked = isLocked,
                icon = if (isLocked) Icons.Default.Lock else null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
        else if (state.shouldShowAdditionalProps(step)) {
            AdditionalPropsProductForm(
                state = state,
                step = step,
                onObjectSelected = onObjectSelected,
                isLocked = isLocked
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Если поле заблокировано, показываем только выбранное значение
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
                label = "Поиск товара (штрихкод, ID, артикул)",
                placeholder = "Введите или отсканируйте"
            )

            if (selectedProduct != null && selectedProduct != plannedProduct) {
                Spacer(modifier = Modifier.height(16.dp))

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
    isLocked: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")

    PlannedObjectCard(
        title = product.product.name,
        subtitle = buildString {
            append("Артикул: ${product.product.articleNumber}")
            if (product.hasExpirationDate()) {
                append("\nСрок годности: ${product.expirationDate?.toLocalDate()?.format(dateFormatter) ?: "Не указан"}")
            }
            append("\nСтатус: ${product.status.format()}")
        },
        isSelected = isSelected,
        onClick = onSelect,
        isLocked = isLocked,
        icon = icon
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

    // Если поле заблокировано, показываем только выбранное значение
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
                Text(
                    text = "ID: ${classifierProduct?.id ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                text = "ID: ${classifierProduct?.id ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (isSelected) {
        Spacer(modifier = Modifier.height(16.dp))

        if (state.shouldShowExpirationDate()) {
            ExpirationDatePicker(
                expirationDate = taskProduct.expirationDate,
                onDateSelected = { date ->
                    hasExpirationDate = date != null
                    onObjectSelected(taskProduct.copy(expirationDate = date))

                    if (date != null && hasStatusSelected) {
                        Timber.d("Автоматический переход после выбора срока годности")
                    }
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ProductStatusSelector(
            selectedStatus = taskProduct.status,
            onStatusSelected = { status ->
                hasStatusSelected = true
                onObjectSelected(taskProduct.copy(status = status))

                if (!needsExpirationDate || hasExpirationDate) {
                    Timber.d("Автоматический переход после выбора статуса")
                }
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
    }
}

@Composable
private fun ManualProductEntryField(
    selectedProduct: TaskProduct?,
    onObjectSelected: (Any) -> Unit
) {
    Text(
        text = "Введите ID товара:",
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = selectedProduct?.product?.id ?: "",
        onValueChange = { id ->
            if (id.isNotEmpty()) {
                val product = Product(id = id, name = "Товар $id")
                val taskProduct = TaskProduct(
                    id = java.util.UUID.randomUUID().toString(),
                    product = product
                )
                onObjectSelected(taskProduct)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ProductClassifierStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val plannedProduct = state.plannedAction?.storageProductClassifier
    val selectedProduct = state.selectedObjects[step.id] as? Product

    var barcodeValue by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var showProductSelector by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Карточка с запланированным объектом
        if (plannedProduct != null) {
            ProductClassifierCard(
                product = plannedProduct,
                isSelected = selectedProduct != null,
                onClick = { onObjectSelected(plannedProduct) },
                isLocked = isLocked,
                icon = if (isLocked) Icons.Default.Lock else null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Если поле заблокировано, показываем только выбранное значение
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
            // Обычный UI с возможностью выбора
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
                label = "Поиск товара (штрихкод, ID, артикул)",
                placeholder = "Введите или отсканируйте"
            )

            if (selectedProduct != null && selectedProduct != plannedProduct) {
                Spacer(modifier = Modifier.height(16.dp))

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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    PlannedObjectCard(
        title = product.name,
        subtitle = "Артикул: ${product.articleNumber}\nID: ${product.id}",
        isSelected = isSelected,
        onClick = onClick,
        isLocked = isLocked,
        icon = icon
    )
}

@Composable
fun BinStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    handleBarcode: (String) -> Unit,
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

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (plannedBin != null) {
            PlannedObjectCard(
                title = "Ячейка: ${plannedBin.code}",
                subtitle = "Зона: ${plannedBin.zone}",
                isSelected = selectedBin != null,
                onClick = { onObjectSelected(plannedBin) },
                isLocked = isLocked,
                icon = if (isLocked) Icons.Default.Lock else null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Если поле заблокировано, показываем только выбранное значение
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
            // Обычный UI с возможностью выбора
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
                placeholder = "Введите или отсканируйте код ячейки"
            )

            if (selectedBin != null && selectedBin != plannedBin) {
                Spacer(modifier = Modifier.height(16.dp))

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

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (plannedPallet != null) {
            PlannedObjectCard(
                title = "Паллета: ${plannedPallet.code}",
                subtitle = if (plannedPallet.isClosed) "Закрыта" else "Открыта",
                isSelected = selectedPallet != null,
                onClick = { onObjectSelected(plannedPallet) },
                isLocked = isLocked,
                icon = if (isLocked) Icons.Default.Lock else null
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Если поле заблокировано, показываем только выбранное значение
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
            // Обычный UI с возможностью выбора
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
                label = if (isStorage) "Код паллеты хранения" else "Код паллеты размещения",
                placeholder = "Введите или отсканируйте код паллеты"
            )

            if (selectedPallet != null && selectedPallet != plannedPallet) {
                Spacer(modifier = Modifier.height(16.dp))

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

    val focusRequester = remember { FocusRequester() }
    var inputValue by remember {
        mutableStateOf(if (selectedQuantity > 0) selectedQuantity.toString() else "")
    }

    // Устанавливаем фокус на поле ввода при первом отображении, если не заблокировано
    LaunchedEffect(Unit) {
        if (!isLocked) {
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

            FormSpacer(8)
        }

        // Если поле заблокировано буфером, показываем только заблокированную карточку
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
                focusRequester = focusRequester
            )
        }

        if (willExceedPlan && plannedQuantity > 0) {
            FormSpacer(8)
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
private fun WarningMessage(message: String) {
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
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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