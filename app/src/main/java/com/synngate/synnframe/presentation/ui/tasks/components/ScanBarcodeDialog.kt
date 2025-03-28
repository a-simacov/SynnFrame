package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.tasks.model.ScanBarcodeDialogState
import com.synngate.synnframe.presentation.util.formatQuantity
import kotlinx.coroutines.delay

@Composable
fun ScanBarcodeDialog(
    onBarcodeScanned: (String) -> Unit,
    onQuantityChange: (TaskFactLine, String) -> Unit,
    onClose: () -> Unit,
    scannedProduct: Product?,
    selectedFactLine: TaskFactLine?,
    scannedBarcode: String?,
    dialogState: ScanBarcodeDialogState,
    onQuantityInputChange: (String) -> Unit,
    onQuantityError: (Boolean) -> Unit,
    onScannerActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(scannedProduct) {
        if (scannedProduct != null && dialogState.additionalQuantity.isEmpty()) {
            onQuantityInputChange("1")
        }
    }

    val additionalQuantity = dialogState.additionalQuantity
    val isError = dialogState.isError
    val isScannerActive = dialogState.isScannerActive

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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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

                if (isScannerActive) {
                    BarcodeScannerView(
                        onBarcodeDetected = onBarcodeScanned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                    )
                } else {
                    // Заглушка пока сканер отключен
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    // Автоматически активируем сканер через некоторое время
                    LaunchedEffect(Unit) {
                        delay(1000)
                        onScannerActiveChange(true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    horizontalAlignment = Alignment.CenterHorizontally // Центрирование содержимого
                ) {
                    if (scannedProduct != null) {
                        // Название товара - по центру
                        Text(
                            text = scannedProduct.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Добавляем ID товара
                        Text(
                            text = "ID: ${scannedProduct.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        // Добавляем единицу измерения и штрихкод
                        val mainUnit = scannedProduct.getMainUnit()
                        if (mainUnit != null) {
                            Text(
                                text = "Ед. измерения: ${mainUnit.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Штрихкод: ${mainUnit.mainBarcode}",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Поля ввода количества с кнопками + и -
                        if (selectedFactLine != null) {
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
                                            onQuantityInputChange(newValue.toString())
                                        } catch (e: Exception) {
                                            onQuantityError(true)
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
                                    onValueChange = {
                                        onQuantityInputChange(it)
                                    },
                                    label = stringResource(R.string.quantity),
                                    isError = isError,
                                    errorText = if (isError) stringResource(R.string.invalid_quantity) else null,
                                    allowNegative = false,
                                    modifier = Modifier.width(120.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                // Кнопка увеличения количества
                                Button(
                                    onClick = {
                                        try {
                                            val currentValue = additionalQuantity.toFloatOrNull() ?: 0f
                                            val newValue = currentValue + 1
                                            onQuantityInputChange(newValue.toString())
                                        } catch (e: Exception) {
                                            onQuantityError(true)
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

                            Spacer(modifier = Modifier.height(24.dp))

                            // Отображение текущего и итогового количества
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Текущее количество - меньшим шрифтом
                                Text(
                                    text = "Текущее количество: ${formatQuantity(selectedFactLine.quantity)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Итоговое количество - крупным шрифтом
                                val totalQuantity = try {
                                    selectedFactLine.quantity + (additionalQuantity.toFloatOrNull() ?: 0f)
                                } catch (e: Exception) {
                                    selectedFactLine.quantity
                                }

                                Text(
                                    text = "Итоговое количество: ${formatQuantity(totalQuantity)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Кнопка применения изменений
                            Button(
                                onClick = {
                                    try {
                                        val addValue = additionalQuantity.toFloatOrNull()
                                        if (addValue != null) {
                                            onQuantityChange(selectedFactLine, additionalQuantity)
                                            // Закрываем диалог при нажатии на "Применить"
                                            onClose()
                                        } else {
                                            onQuantityError(true)
                                        }
                                    } catch (e: Exception) {
                                        onQuantityError(true)
                                    }
                                },
                                enabled = additionalQuantity.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.apply),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    } else if (scannedBarcode != null) {
                        Text(
                            text = stringResource(R.string.product_not_found_by_barcode, scannedBarcode),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.scan_barcode_to_find_product),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier
                    .weight(0.1f)
                    .height(8.dp))
            }
        }
    }
}