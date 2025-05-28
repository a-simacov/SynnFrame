package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityColumn
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardQuantityInput
import com.synngate.synnframe.presentation.ui.wizard.action.components.formatQuantityDisplay
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun StorageProductStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit
) {
    val plannedProduct = state.plannedAction?.storageProduct

    Column(modifier = Modifier.fillMaxWidth()) {
        if (plannedProduct != null) {
            StorageProductCard(
                product = plannedProduct,
                isSelected = state.selectedObjects[step.id] != null,
                onSelect = { onObjectSelected(plannedProduct) }
            )
        } else if (state.shouldShowAdditionalProductProps(step)) {
            AdditionalPropsProductForm(
                state = state,
                step = step,
                onObjectSelected = onObjectSelected
            )
        } else {
            ManualProductEntryField(
                selectedProduct = state.selectedObjects[step.id] as? TaskProduct,
                onObjectSelected = onObjectSelected
            )
        }
    }
}

@Composable
private fun StorageProductCard(
    product: TaskProduct,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.product.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Артикул: ${product.product.articleNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (product.hasExpirationDate()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Срок годности: ${product.expirationDate?.toLocalDate()?.format(dateFormatter) ?: "Не указан"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.expirationDate?.isBefore(LocalDateTime.now()) == true)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Статус: ${product.status.format()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (product.status) {
                        ProductStatus.STANDARD -> MaterialTheme.colorScheme.onSurface
                        ProductStatus.DEFECTIVE -> MaterialTheme.colorScheme.error
                        ProductStatus.EXPIRED -> Color(0xFFFF9800) // Оранжевый для просроченных
                    }
                )
            }

            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = if (isSelected)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Выбрать"
                )
            }
        }
    }
}

@Composable
private fun AdditionalPropsProductForm(
    state: ActionWizardState,
    step: ActionStepTemplate,
    onObjectSelected: (Any) -> Unit
) {
    val classifierProduct = state.plannedAction?.storageProductClassifier
    val taskProduct = state.getTaskProductFromClassifier(step.id)
    val isSelected = state.selectedObjects[step.id] != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

            IconButton(
                onClick = { onObjectSelected(taskProduct) }
            ) {
                Icon(
                    imageVector = if (isSelected)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Выбрать"
                )
            }
        }
    }

    if (isSelected) {
        Spacer(modifier = Modifier.height(16.dp))

        if (state.shouldShowExpirationDate()) {
            ExpirationDatePicker(
                expirationDate = taskProduct.expirationDate,
                onDateSelected = { date ->
                    onObjectSelected(taskProduct.copy(expirationDate = date))
                },
                isRequired = true
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ProductStatusSelector(
            selectedStatus = taskProduct.status,
            onStatusSelected = { status ->
                onObjectSelected(taskProduct.copy(status = status))
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
fun StorageProductClassifierStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit
) {
    val plannedProduct = state.plannedAction?.storageProductClassifier
    val selectedProduct = state.selectedObjects[step.id] as? Product

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (plannedProduct != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedProduct != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plannedProduct.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Артикул: ${plannedProduct.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(
                        onClick = { onObjectSelected(plannedProduct) }
                    ) {
                        Icon(
                            imageVector = if (selectedProduct != null)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Выбрать"
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Нет запланированного товара. Введите ID товара:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = selectedProduct?.id ?: "",
                onValueChange = { id ->
                    if (id.isNotEmpty()) {
                        val product = Product(id = id, name = "Товар $id")
                        onObjectSelected(product)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BinStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    isStorage: Boolean
) {
    val plannedBin = if (isStorage)
        state.plannedAction?.storageBin
    else
        state.plannedAction?.placementBin

    val selectedBin = state.selectedObjects[step.id] as? BinX

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (plannedBin != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedBin != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ячейка: ${plannedBin.code}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Зона: ${plannedBin.zone}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(
                        onClick = { onObjectSelected(plannedBin) }
                    ) {
                        Icon(
                            imageVector = if (selectedBin != null)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Выбрать"
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Введите код ячейки:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = selectedBin?.code ?: "",
                onValueChange = { code ->
                    if (code.isNotEmpty()) {
                        // Создаем простой объект с кодом
                        val bin = BinX(code = code, zone = "Неизвестно")
                        onObjectSelected(bin)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PalletStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit,
    isStorage: Boolean
) {
    val plannedPallet = if (isStorage)
        state.plannedAction?.storagePallet
    else
        state.plannedAction?.placementPallet

    val selectedPallet = state.selectedObjects[step.id] as? Pallet

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (plannedPallet != null) {
            // Отображаем паллету из плана
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedPallet != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Паллета: ${plannedPallet.code}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (plannedPallet.isClosed) "Закрыта" else "Открыта",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(
                        onClick = { onObjectSelected(plannedPallet) }
                    ) {
                        Icon(
                            imageVector = if (selectedPallet != null)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Выбрать"
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Введите код паллеты:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = selectedPallet?.code ?: "",
                onValueChange = { code ->
                    if (code.isNotEmpty()) {
                        // Создаем простой объект с кодом
                        val pallet = Pallet(code = code)
                        onObjectSelected(pallet)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuantityStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onQuantityChanged: (Float) -> Unit,
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

    // Устанавливаем фокус на поле ввода при первом отображении
    LaunchedEffect(Unit) {
        try {
            delay(100)
            focusRequester.requestFocus()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при установке фокуса на поле ввода количества")
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

        WizardQuantityInput(
            value = inputValue,
            onValueChange = { newValue ->
                inputValue = newValue
                newValue.toFloatOrNull()?.let { onQuantityChanged(it) }
            },
            onIncrement = {
                val newValue = (inputValue.toFloatOrNull() ?: 0f) + 1f
                inputValue = newValue.toString()
                onQuantityChanged(newValue)
            },
            onDecrement = {
                val currentValue = inputValue.toFloatOrNull() ?: 0f
                if (currentValue > 1) {
                    val newValue = currentValue - 1f
                    inputValue = newValue.toString()
                    onQuantityChanged(newValue)
                }
            },
            onImeAction = {
                // Сохраняем результат при нажатии на кнопку клавиатуры
                inputValue.toFloatOrNull()?.let { onQuantityChanged(it) }
            },
            isError = state.error != null,
            errorText = state.error,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            label = step.promptText,
            focusRequester = focusRequester
        )

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