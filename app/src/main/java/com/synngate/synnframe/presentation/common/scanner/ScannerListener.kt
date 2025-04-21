package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synngate.synnframe.presentation.common.LocalScannerService

@Composable
fun ScannerListener(
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scannerService = LocalScannerService.current

    scannerService?.let {
        if (it.hasRealScanner()) {
            it.ScannerEffect(onScanResult = onBarcodeScanned)
        }
    }
}

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