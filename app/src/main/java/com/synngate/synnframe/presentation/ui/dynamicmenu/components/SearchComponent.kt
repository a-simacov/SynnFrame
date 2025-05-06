package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import timber.log.Timber

class SearchComponent(
    private val searchValue: String,
    private val onSearchValueChanged: (String) -> Unit,
    private val onSearch: () -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        val scannerService = LocalScannerService.current
        val hasRealScanner = scannerService?.hasRealScanner() ?: false
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        // Генерируем уникальный ключ для шага, используя хэш значений
        val stepKey = remember(searchValue) { "search_${searchValue.hashCode()}" }

        // Обработчик внешнего сканера штрихкодов
        BarcodeHandlerWithState(
            stepKey = stepKey,
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")
                onSearchValueChanged(barcode)
                onSearch()
                setProcessingState(false)
            }
        )

//        ScannerListener(
//            onBarcodeScanned = { barcode ->
//                if (barcode.isNotEmpty()) {
//                    Timber.d("Получен штрихкод от сервиса сканера: $barcode")
//                    onSearchValueChanged(barcode)
//                    onSearch()
//                }
//            }
//        )

        // Диалог сканирования с камеры
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    Timber.d("Получен штрихкод от камеры: $barcode")
                    onSearchValueChanged(barcode)
                    onSearch()
                    showCameraScannerDialog = false
                },
                onClose = {
                    showCameraScannerDialog = false
                },
                instructionText = stringResource(R.string.scan_barcode_instruction),
                allowManualInput = true
            )
        }

        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchTextField(
                    value = searchValue,
                    onValueChange = onSearchValueChanged,
                    label = stringResource(id = R.string.search_value),
                    onSearch = onSearch,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showCameraScannerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_with_camera)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.search))
            }
        }
    }

    override fun usesWeight(): Boolean = false
}