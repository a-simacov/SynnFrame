package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog

/**
 * Компонент поиска с возможностью сохранения значения через отдельный диалог
 */
class SearchSaveableComponent(
    private val searchValue: String,
    private val onSearchValueChanged: (String) -> Unit,
    private val onSearch: () -> Unit,
    private val savedSearchKey: String?,
    private val hasValidSavedSearchKey: Boolean,
    private val onClearSavedKey: () -> Unit,
    private val onAddSavedKey: () -> Unit
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

        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            // Поле поиска
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

            Spacer(modifier = Modifier.height(8.dp))

            // Отображение сохраненного ключа поиска или кнопка добавления
            if (hasValidSavedSearchKey && !savedSearchKey.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Saved key:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = savedSearchKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        IconButton(
                            onClick = onClearSavedKey,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear saved key",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else {
                // Кнопка для добавления сохраняемого ключа
                TextButton(
                    onClick = onAddSavedKey,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add saved key")
                }
            }
        }
    }

    override fun usesWeight(): Boolean = false
}