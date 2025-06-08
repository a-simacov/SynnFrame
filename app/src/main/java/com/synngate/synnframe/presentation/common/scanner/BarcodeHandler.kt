package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.synngate.synnframe.presentation.common.LocalScannerService

@Composable
fun BarcodeHandlerWithState(
    stepKey: Any,
    stepResult: Any? = null,
    onBarcodeScanned: (String, (Boolean) -> Unit) -> Unit
) {
    var isProcessingBarcode by remember(stepKey, stepResult) { mutableStateOf(false) }

    val scannerService = LocalScannerService.current

    val hasRealScanner = scannerService?.hasRealScanner() ?: false

    LaunchedEffect(stepKey) {
        isProcessingBarcode = false

        if (hasRealScanner) {
            scannerService?.let { service ->
                // Проверяем, что это не камера устройства, прежде чем активировать
                if (!service.isEnabled() && !service.isCameraScanner()) {
                    service.enable()
                }
            }
        }
    }

    val setProcessingState = { newState: Boolean ->
        isProcessingBarcode = newState
    }

    if (hasRealScanner) {
        ScannerListener(
            onBarcodeScanned = { barcode ->
                if (!isProcessingBarcode) {
                    isProcessingBarcode = true
                    onBarcodeScanned(barcode, setProcessingState)
                }
            }
        )
    }

    DisposableEffect(stepKey) {
        onDispose {
            if (isProcessingBarcode) {
                isProcessingBarcode = false
            }
        }
    }
}