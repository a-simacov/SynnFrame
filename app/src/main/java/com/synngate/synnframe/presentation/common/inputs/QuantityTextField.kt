package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun QuantityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    precision: Int = 3,
    allowNegative: Boolean = true
) {
    val regexPattern = if (allowNegative) {
        "^-?\\d*\\.?\\d{0,$precision}\$"
    } else {
        "^\\d*\\.?\\d{0,$precision}\$"
    }

    AppTextField(
        value = value,
        onValueChange = { newValue ->
            // Разрешаем только цифры, точку и отрицательные числа (если разрешено)
            if (newValue.isEmpty() ||
                (allowNegative && newValue == "-") ||
                newValue.matches(Regex(regexPattern))
            ) {
                onValueChange(newValue)
            }
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        keyboardType = KeyboardType.Decimal,
        imeAction = ImeAction.Done
    )
}