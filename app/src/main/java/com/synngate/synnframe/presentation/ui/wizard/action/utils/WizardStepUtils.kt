package com.synngate.synnframe.presentation.ui.wizard.action.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.wizard.action.components.AutoFillIndicator
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardBarcodeField

object WizardStepUtils {

    @Composable
    fun StandardBarcodeField(
        value: String,
        onValueChange: (String) -> Unit,
        onSearch: () -> Unit,
        onScannerClick: () -> Unit,
        modifier: Modifier = Modifier,
        label: String = "Введите штрихкод",
        isError: Boolean = false,
        errorText: String? = null,
        onSelectFromList: (() -> Unit)? = null,
        placeholder: String? = null,
        isAutoFilled: Boolean = false,
        autoFillSource: String? = null
    ) {
        Column(modifier = modifier) {
            WizardBarcodeField(
                value = value,
                onValueChange = onValueChange,
                onSearch = onSearch,
                onScannerClick = onScannerClick,
                modifier = Modifier.fillMaxWidth(),
                label = label,
                isError = isError,
                errorText = errorText,
                onSelectFromList = onSelectFromList,
                placeholder = placeholder
            )

            if (isAutoFilled) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    AutoFillIndicator(source = autoFillSource)
                }
            }
        }
    }
}