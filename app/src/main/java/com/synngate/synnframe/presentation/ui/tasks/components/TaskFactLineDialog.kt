package com.synngate.synnframe.presentation.ui.tasks.components

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.ui.tasks.model.FactLineDialogState
import com.synngate.synnframe.presentation.util.formatQuantity

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
                DialogHeader(onDismiss)

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                Spacer(modifier = Modifier
                    .weight(0.5f)
                    .height(8.dp))

                ProductInfo(product)

                PlanQuantity(planQuantity)

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                CurrentQuantity(
                    factLine,
                    additionalQuantity,
                    onQuantityChange,
                    onError,
                    isError,
                    errorText
                )

                TotalQuantity(factLine, additionalQuantity, planQuantity)

                Spacer(modifier = Modifier.weight(0.5f))

                ApplyButton(additionalQuantity, onApply, factLine, onError)

                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaskFactLineDialogPreview() {
    SynnFrameTheme {
        TaskFactLineDialog(
            factLine = TaskFactLine(
                id = "1",
                taskId = "4646143131",
                productId = "0000222",
                quantity = 123f
            ),
            product = Product(
                id = "0000222",
                name = "This is a long name of prduct maybe will take some lines",
                accountingModel = AccountingModel.QTY,
                articleNumber = "ART-0002",
                mainUnitId = "987987987987"
            ),
            planQuantity = 504f,
            dialogState = FactLineDialogState(),
            onQuantityChange = {},
            onError = {},
            onApply = { _, _ ->
            },
            onDismiss = { }
        )
    }
}

@Composable
private fun ApplyButton(
    additionalQuantity: String,
    onApply: (TaskFactLine, String) -> Unit,
    factLine: TaskFactLine,
    onError: (Boolean) -> Unit
) {
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
}

@Composable
private fun TotalQuantity(
    factLine: TaskFactLine,
    additionalQuantity: String,
    planQuantity: Float
) {
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
}

@Composable
private fun CurrentQuantity(
    factLine: TaskFactLine,
    additionalQuantity: String,
    onQuantityChange: (String) -> Unit,
    onError: (Boolean) -> Unit,
    isError: Boolean,
    errorText: String
) {
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
}

@Composable
private fun PlanQuantity(planQuantity: Float) {
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
}

@Composable
private fun ProductInfo(product: Product?) {
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
}

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
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
}