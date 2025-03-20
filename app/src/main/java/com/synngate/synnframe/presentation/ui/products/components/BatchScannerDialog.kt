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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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

/**
 * Данные о результате сканирования
 */
data class ScanResult(
    val barcode: String,
    val product: Product?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Диалог для пакетного сканирования товаров
 *
 * @param onBarcodeScanned Функция, вызываемая при сканировании штрихкода. Она запускает асинхронный поиск
 *                         товара и не возвращает результат. Результат будет обработан внутри
 */
@Composable
fun BatchScannerDialog(
    onBarcodeScanned: (String) -> Unit, // Изменена сигнатура - теперь функция ничего не возвращает
    onClose: () -> Unit,
    onDone: (List<ScanResult>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Список результатов сканирования
    val scanResults = remember { mutableStateListOf<ScanResult>() }

    // Флаг выполнения поиска товара
    var isSearching by remember { mutableStateOf(false) }

    // Флаг для управления сканером
    var isScannerActive by remember { mutableStateOf(true) }

    // Последний отсканированный штрихкод
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
                // Заголовок диалога
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.batch_scanning),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    // Кнопка закрытия
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                // Область результатов сканирования
                Text(
                    text = stringResource(id = R.string.scanned_items, scanResults.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Показываем список отсканированных товаров
                LazyColumn(
                    modifier = Modifier
                        .weight(0.35f)
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
                                    // Название товара или штрихкод, если товар не найден
                                    Text(
                                        text = result.product?.name ?: result.barcode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.product != null)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.error
                                    )

                                    // Штрихкод
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
                                }

                                // Кнопка удаления результата
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

                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Область со статусом текущего сканирования
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    if (isSearching) {
                        // Показываем индикатор поиска
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
                    } else {
                        // Показываем инструкцию
                        Text(
                            text = stringResource(id = R.string.scan_barcode_instruction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Область превью камеры
                if (isScannerActive) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxWidth()
                    ) {
                        BarcodeScannerView(
                            onBarcodeDetected = { barcode ->
                                if (!isSearching) {
                                    isSearching = true
                                    lastScannedBarcode = barcode

                                    // Вызываем обработчик сканирования, который запустит асинхронный поиск
                                    onBarcodeScanned(barcode)

                                    // Добавляем запись в список пока без информации о товаре
                                    // Продукт будет null, пока не получим результат поиска
                                    scanResults.add(ScanResult(barcode, null))

                                    // Сбрасываем состояние
                                    isSearching = false
                                    lastScannedBarcode = null
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

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка очистки результатов
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

                    // Кнопка завершения сканирования
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
            }
        }
    }
}