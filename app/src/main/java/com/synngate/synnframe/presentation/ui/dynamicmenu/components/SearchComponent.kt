package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog

class SearchComponent(
    private val searchValue: String,
    private val onSearchValueChanged: (String) -> Unit,
    private val onSearch: () -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        val stepKey = remember(searchValue) { "search_${searchValue.hashCode()}" }

        BarcodeHandlerWithState(
            stepKey = stepKey,
            onBarcodeScanned = { barcode, setProcessingState ->
                onSearchValueChanged(barcode)
                onSearch()
                setProcessingState(false)
            }
        )

        ScannerListener(
            onBarcodeScanned = { barcode ->
                if (barcode.isNotEmpty()) {
                    onSearchValueChanged(barcode)
                    onSearch()
                }
            }
        )

        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
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