package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.tasks.model.ScanningState
import kotlinx.coroutines.delay

/**
 * Диалог сканирования штрихкодов (только сканирование, без ввода количества)
 */
@Composable
fun ScanBarcodeDialog(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    scannerMessage: String? = null,
    scanningState: ScanningState = ScanningState.IDLE,
    isScannerActive: Boolean = true,
    onScannerActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isScannerActive) {
        if (!isScannerActive) {
            delay(1000)
            onScannerActiveChange(true)
        }
    }

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
                // Заголовок зависит от текущего состояния
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(scanningState) {
                            ScanningState.SCAN_PRODUCT -> stringResource(id = R.string.scan_product)
                            ScanningState.SCAN_BIN -> stringResource(id = R.string.scan_bin)
                            else -> stringResource(id = R.string.scan_barcode)
                        },
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

                // Визуальное выделение типа сканирования
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when(scanningState) {
                            ScanningState.SCAN_PRODUCT -> MaterialTheme.colorScheme.primaryContainer
                            ScanningState.SCAN_BIN -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = when(scanningState) {
                            ScanningState.SCAN_PRODUCT -> "Сканируйте штрихкод товара"
                            ScanningState.SCAN_BIN -> "Сканируйте штрихкод ячейки"
                            else -> "Отсканируйте штрихкод"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScannerActive) {
                        BarcodeScannerView(
                            onBarcodeDetected = onBarcodeScanned,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Заглушка пока сканер отключен
                        CircularProgressIndicator()
                    }
                }

                // Сообщение о результате сканирования или ошибке
                if (!scannerMessage.isNullOrEmpty()) {
                    Text(
                        text = scannerMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.1f).height(8.dp))
            }
        }
    }
}