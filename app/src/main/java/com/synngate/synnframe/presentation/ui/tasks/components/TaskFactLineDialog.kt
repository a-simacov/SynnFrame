package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.ui.tasks.model.FactLineDialogState
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Диалог для изменения количества продукта в строке факта
 */
@Composable
fun TaskFactLineDialog(
    factLine: TaskFactLine,
    product: Product?,
    planQuantity: Float,
    dialogState: FactLineDialogState,
    onQuantityChange: (String) -> Unit,
    onError: (Boolean) -> Unit,
    onApply: (TaskFactLine, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val additionalQuantity = dialogState.additionalQuantity
    val isError = dialogState.isError
    val errorText = stringResource(R.string.invalid_quantity)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_quantity),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                // Информация о товаре
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = product?.name ?: stringResource(R.string.unknown_product),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Добавляем ID товара и единицу измерения, если доступны
                    if (product != null) {
                        Text(
                            text = stringResource(R.string.product_id_fmt, product.id),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        val mainUnit = product.getMainUnit()
                        if (mainUnit != null) {
                            Text(
                                text = stringResource(R.string.unit_fmt, mainUnit.name),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Информация о плановом количестве
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.plan_quantity),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = formatQuantity(planQuantity),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                // Блок с текущим количеством и полями ввода
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.current_quantity),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = formatQuantity(factLine.quantity),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Строка с кнопками +/- и полем ввода
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Кнопка уменьшения количества
                        Button(
                            onClick = {
                                try {
                                    val currentValue = additionalQuantity.toFloatOrNull() ?: 0f
                                    val newValue = currentValue - 1
                                    onQuantityChange(newValue.toString())
                                } catch (e: Exception) {
                                    onError(true)
                                }
                            },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Поле ввода количества
                        QuantityTextField(
                            value = additionalQuantity,
                            onValueChange = onQuantityChange,
                            label = stringResource(R.string.add_quantity),
                            isError = isError,
                            errorText = if (isError) errorText else null,
                            allowNegative = true,
                            modifier = Modifier.width(160.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Кнопка увеличения количества
                        Button(
                            onClick = {
                                try {
                                    val currentValue = additionalQuantity.toFloatOrNull() ?: 0f
                                    val newValue = currentValue + 1
                                    onQuantityChange(newValue.toString())
                                } catch (e: Exception) {
                                    onError(true)
                                }
                            },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }

                // Отображение итогового количества
                val totalQuantity = try {
                    factLine.quantity + (additionalQuantity.toFloatOrNull() ?: 0f)
                } catch (e: Exception) {
                    factLine.quantity
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.result_quantity_label),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = formatQuantity(totalQuantity),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Процент от плана
                    if (planQuantity > 0) {
                        val percentage = (totalQuantity / planQuantity * 100)
                        Text(
                            text = stringResource(R.string.percentage_of_plan, percentage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (percentage >= 100)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Кнопка применения изменений
                Button(
                    onClick = {
                        try {
                            val addValue = additionalQuantity.toFloatOrNull()
                            if (addValue != null && addValue != 0f) {
                                onApply(factLine, additionalQuantity)
                            } else {
                                onError(true)
                            }
                        } catch (e: Exception) {
                            onError(true)
                        }
                    },
                    enabled = additionalQuantity.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(text = stringResource(R.string.apply))
                }

                Spacer(modifier = Modifier.weight(0.3f).height(8.dp))
            }
        }
    }
}