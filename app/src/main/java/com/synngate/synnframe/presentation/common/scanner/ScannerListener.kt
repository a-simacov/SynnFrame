package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synngate.synnframe.presentation.common.LocalScannerService

/**
 * Компонент для простой интеграции сканера в любой экран
 */
@Composable
fun ScannerListener(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Получаем доступ к сервису сканера через CompositionLocal
    val scannerService = LocalScannerService.current

    scannerService?.ScannerEffect(onScanResult = onBarcodeScanned)
}

/**
 * Компонент для отображения кнопки сканирования
 */
@Composable
fun ScanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Scan Barcode"
        )
    }
}