package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
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

        BarcodeHandlerWithState(
            stepKey = stepKey,
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")
                onSearchValueChanged(barcode)
                onSearch()
                setProcessingState(false)
            }
        )

        ScannerListener(
            onBarcodeScanned = { barcode ->
                if (barcode.isNotEmpty()) {
                    Timber.d("Получен штрихкод от сервиса сканера: $barcode")
                    onSearchValueChanged(barcode)
                    onSearch()
                }
            }
        )

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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
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
    }

    override fun usesWeight(): Boolean = false
}

@Preview(apiLevel = 34)
@Composable
private fun Demo() {
    SynnFrameTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchTextField(
                value = "searchValue",
                onValueChange = { },
                label = stringResource(id = R.string.search_value),
                onSearch = {},
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_with_camera)
                        )
                    }
                }
            )
            IconButton(
                onClick = { },
                //modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_value)
                )
            }
        }
    }
}