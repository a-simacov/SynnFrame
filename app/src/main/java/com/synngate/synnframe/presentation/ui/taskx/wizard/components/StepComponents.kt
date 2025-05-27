package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.NumberTextField
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import java.util.UUID

@Composable
fun StorageProductStep(
    step: ActionStepTemplate,
    state: ActionWizardState,
    onObjectSelected: (Any) -> Unit
) {
    val plannedProduct = state.plannedAction?.storageProduct
    val selectedProduct = state.selectedObjects[step.id] as? TaskProduct

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (plannedProduct != null) {
            // Отображаем продукт из плана
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
                            text = plannedProduct.product.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Артикул: ${plannedProduct.product.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Кнопка выбора
                    IconButton(
                        onClick = {
                            val taskProduct = if (step.inputAdditionalProps) {
                                // Если нужно вводить доп. свойства, создаем копию с текущими свойствами
                                TaskProduct(
                                    id = plannedProduct.id,
                                    product = plannedProduct.product,
                                    expirationDate = selectedProduct?.expirationDate ?: plannedProduct.expirationDate,
                                    status = selectedProduct?.status ?: plannedProduct.status
                                )
                            } else {
                                // Иначе используем как есть
                                plannedProduct
                            }
                            onObjectSelected(taskProduct)
                        }
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

            // Если требуется ввод доп. свойств и объект выбран
            if (step.inputAdditionalProps && selectedProduct != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Срок годности
                ExpirationDatePicker(
                    expirationDate = selectedProduct.expirationDate,
                    onDateSelected = { date ->
                        onObjectSelected(selectedProduct.copy(expirationDate = date))
                    },
                    isRequired = plannedProduct.product.usesExpDate()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Статус товара
                ProductStatusSelector(
                    selectedStatus = selectedProduct.status,
                    onStatusSelected = { status ->
                        onObjectSelected(selectedProduct.copy(status = status))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Если нет запланированного продукта, показываем поле ввода
            Text(
                text = "Нет запланированного товара. Введите ID товара:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = selectedProduct?.product?.id ?: "",
                onValueChange = { id ->
                    if (id.isNotEmpty()) {
                        // Создаем простой объект с ID
                        val product = Product(id = id, name = "Товар $id")
                        val taskProduct = TaskProduct(
                            id = UUID.randomUUID().toString(),
                            product = product
                        )
                        onObjectSelected(taskProduct)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
            // Отображаем продукт из плана
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

                    // Кнопка выбора
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
            // Если нет запланированного продукта, показываем поле ввода
            Text(
                text = "Нет запланированного товара. Введите ID товара:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = selectedProduct?.id ?: "",
                onValueChange = { id ->
                    if (id.isNotEmpty()) {
                        // Создаем простой объект с ID
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
            // Отображаем ячейку из плана
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

                    // Кнопка выбора
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
            // Если нет запланированной ячейки, показываем поле ввода
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

                    // Кнопка выбора
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
            // Если нет запланированной паллеты, показываем поле ввода
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
    onQuantityChanged: (Float) -> Unit
) {
    val plannedQuantity = state.plannedAction?.quantity ?: 0f
    val selectedQuantity = (state.selectedObjects[step.id] as? Number)?.toFloat() ?: 0f

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (plannedQuantity > 0) {
            Text(
                text = "Плановое количество: $plannedQuantity",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Введите количество:",
            style = MaterialTheme.typography.bodyMedium
        )

        NumberTextField(
            value = if (selectedQuantity > 0) selectedQuantity.toString() else "",
            onValueChange = { value ->
                val number = value.toFloatOrNull()
                if (number != null && number > 0) {
                    onQuantityChanged(number)
                }
            },
            label = "Количество",
            modifier = Modifier.fillMaxWidth()
        )
    }
}