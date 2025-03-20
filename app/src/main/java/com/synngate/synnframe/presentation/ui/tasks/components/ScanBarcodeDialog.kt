package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.util.formatQuantity

/**
 * Диалог сканирования штрихкодов для заданий
 */
@Composable
fun ScanBarcodeDialog(
    onBarcodeScanned: (String) -> Unit,
    onQuantityChange: (TaskFactLine, String) -> Unit,
    onClose: () -> Unit,
    scannedProduct: Product?,
    selectedFactLine: TaskFactLine?,
    scannedBarcode: String?,
    modifier: Modifier = Modifier
) {
    var additionalQuantity by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val errorText = stringResource(R.string.invalid_quantity)

    Dialog(
        onDismissRequest = onClose,
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
                    .padding(16.dp)
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.scan_barcode),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Информация о товаре и поля ввода количества
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                ) {
                    // Название товара
                    Text(
                        text = scannedProduct?.name ?:
                        if (scannedBarcode != null)
                            stringResource(R.string.product_not_found_by_barcode, scannedBarcode)
                        else
                            stringResource(R.string.scan_barcode_to_find_product),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поля ввода количества (видимы только если товар найден и строка факта доступна)
                    if (scannedProduct != null && selectedFactLine != null) {
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
                                    text = formatQuantity(selectedFactLine.quantity),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            // Добавляемое количество
                            Column(modifier = Modifier.weight(2f)) {
                                QuantityTextField(
                                    value = additionalQuantity,
                                    onValueChange = {
                                        additionalQuantity = it
                                        isError = false
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
                            selectedFactLine.quantity + (additionalQuantity.toFloatOrNull() ?: 0f)
                        } catch (e: Exception) {
                            selectedFactLine.quantity
                        }

                        Text(
                            text = stringResource(R.string.result_quantity, formatQuantity(totalQuantity)),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Кнопка изменения количества
                        Button(
                            onClick = {
                                try {
                                    val addValue = additionalQuantity.toFloatOrNull()
                                    if (addValue != null && addValue != 0f) {
                                        onQuantityChange(selectedFactLine, additionalQuantity)
                                        additionalQuantity = ""
                                    } else {
                                        isError = true
                                    }
                                } catch (e: Exception) {
                                    isError = true
                                }
                            },
                            enabled = additionalQuantity.isNotEmpty(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = stringResource(R.string.apply))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Область сканирования
                BarcodeScannerView(
                    onBarcodeDetected = onBarcodeScanned,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                )
            }
        }
    }
}