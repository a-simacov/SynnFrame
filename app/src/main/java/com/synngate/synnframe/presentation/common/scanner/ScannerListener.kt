// Файл: app/src/main/java/com/synngate/synnframe/presentation/common/scanner/ScannerListener.kt

package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.data.barcodescanner.ScannerState
import com.synngate.synnframe.domain.common.ScanError
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener

@Composable
fun ScannerListener(
    scannerService: ScannerService,
    onScanResult: (ScanResult) -> Unit,
    onScanError: ((ScanError) -> Unit)? = null,
    showStatus: Boolean = false,
    statusOnlyOnError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scannerState by scannerService.scannerState.collectAsState()

    // Эффект для управления подключением к сканеру при монтировании/размонтировании компонента
    DisposableEffect(scannerService) {
        val listener = object : ScanResultListener {
            override fun onScanSuccess(result: ScanResult) {
                onScanResult(result)
            }

            override fun onScanError(error: ScanError) {
                onScanError?.invoke(error)
            }
        }

        // Добавляем слушателя при монтировании
        scannerService.addListener(listener)

        // Удаляем слушателя при размонтировании
        onDispose {
            scannerService.removeListener(listener)
        }
    }

    Column(modifier = modifier) {
        // Отображаем статус сканера, если это включено
        if (showStatus) {
            // Если включен режим "только при ошибке", проверяем состояние
            val shouldShowStatus = if (statusOnlyOnError) {
                scannerState is ScannerState.Error
            } else {
                true
            }

            if (shouldShowStatus) {
                ScannerStatusIndicator(
                    scannerService = scannerService,
                    showText = true
                )
            }
        }
    }
}