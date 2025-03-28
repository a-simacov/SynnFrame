package com.synngate.synnframe.presentation.ui.products.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanResult(
    val barcode: String,
    val product: Product?,
    val timestamp: Long = System.currentTimeMillis(),
    val scanCount: Int = 1 // Количество сканирований
)

/**
 * Диалог для пакетного сканирования товаров с поддержкой счетчика повторных сканирований
 *
 * @param onBarcodeScanned Функция, вызываемая при сканировании штрихкода.
 *                         Принимает штрихкод и функцию обратного вызова для обновления найденного товара
 * @param onClose Функция для закрытия диалога
 * @param onDone Функция обратного вызова при завершении сканирования, возвращает список результатов
 */
@Composable
fun BatchScannerDialog(
    onBarcodeScanned: (String, (Product?) -> Unit) -> Unit,
    onClose: () -> Unit,
    onDone: (List<ScanResult>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scanResults = remember { mutableStateListOf<ScanResult>() }
    var isSearching by remember { mutableStateOf(false) }
    val isScannerActive by remember { mutableStateOf(true) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.batch_scanning),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                if (isScannerActive) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxSize()
                    ) {
                        BarcodeScannerView(
                            onBarcodeDetected = { barcode ->
                                if (!isSearching) {
                                    isSearching = true
                                    lastScannedBarcode = barcode

                                    // Проверяем, есть ли уже такой штрихкод в списке
                                    val existingIndex = scanResults.indexOfFirst { it.barcode == barcode }

                                    if (existingIndex >= 0) {
                                        // Если штрихкод уже есть, увеличиваем счетчик и обновляем timestamp
                                        val existingResult = scanResults[existingIndex]
                                        val updatedResult = existingResult.copy(
                                            scanCount = existingResult.scanCount + 1,
                                            timestamp = System.currentTimeMillis()
                                        )

                                        // Обновляем запись в списке
                                        scanResults[existingIndex] = updatedResult

                                        // Сбрасываем состояние поиска
                                        isSearching = false
                                        lastScannedBarcode = null
                                    } else {
                                        // Если штрихкода еще нет, добавляем новую запись
                                        val resultIndex = scanResults.size
                                        scanResults.add(ScanResult(barcode, null))

                                        // Вызываем обработчик сканирования с колбеком для обновления товара
                                        onBarcodeScanned(barcode) { product ->
                                            if (resultIndex < scanResults.size) {
                                                val updatedResult = scanResults[resultIndex].copy(product = product)
                                                scanResults[resultIndex] = updatedResult
                                            }
                                        }

                                        // Сбрасываем состояние поиска
                                        isSearching = false
                                        lastScannedBarcode = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Заглушка пока сканер не активен
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Text(
                    text = stringResource(id = R.string.scanned_items, scanResults.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(8.dp)
                ) {
                    if (scanResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.no_scanned_items),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        items(scanResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = result.product?.name ?: result.barcode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.product != null)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.error
                                    )

                                    if (result.product != null) {
                                        Text(
                                            text = result.barcode,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(id = R.string.product_not_found),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    // Добавляем отображение количества сканирований и времени
                                    Text(
                                        text = "Сканировано: ${result.scanCount} раз (${formatTimestamp(result.timestamp)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { scanResults.remove(result) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.remove),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(
                                id = R.string.searching_product,
                                lastScannedBarcode ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { scanResults.clear() },
                        enabled = scanResults.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = stringResource(id = R.string.clear_all))
                    }

                    Button(
                        onClick = { onDone(scanResults.toList()) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = stringResource(id = R.string.finish_scanning))
                    }
                }

                Spacer(modifier = Modifier
                    .weight(0.1f)
                    .height(8.dp))
            }
        }
    }
}

/**
 * Форматирует временную метку в читаемый вид (часы:минуты:секунды)
 */
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}