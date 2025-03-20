package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.ui.tasks.model.FactLineDialogState
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Диалог редактирования строки факта задания
 */
@Composable
fun TaskFactLineDialog(
    factLine: TaskFactLine,
    product: Product?,
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Наименование товара
                Text(
                    text = product?.name ?: stringResource(R.string.unknown_product),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Блок с текущим и добавляемым количеством
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Текущее количество
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.current_quantity),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = formatQuantity(factLine.quantity),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Добавляемое количество
                    Column(modifier = Modifier.weight(2f)) {
                        // Используем функции из ViewModel
                        QuantityTextField(
                            value = additionalQuantity,
                            onValueChange = {
                                onQuantityChange(it)
                            },
                            label = stringResource(R.string.add_quantity),
                            isError = isError,
                            errorText = if (isError) errorText else null,
                            allowNegative = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Результирующее количество
                val totalQuantity = try {
                    factLine.quantity + (additionalQuantity.toFloatOrNull() ?: 0f)
                } catch (e: Exception) {
                    factLine.quantity
                }

                Text(
                    text = stringResource(R.string.result_quantity, formatQuantity(totalQuantity)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                        enabled = additionalQuantity.isNotEmpty()
                    ) {
                        Text(text = stringResource(R.string.apply))
                    }

                }
            }
        }
    }
}