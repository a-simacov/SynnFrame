package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    AppTextField(
        value = value,
        onValueChange = { newValue ->
            // Разрешаем только цифры и точку для дробных чисел
            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        maxLines = maxLines,
        singleLine = singleLine,
        keyboardType = KeyboardType.Number,
        imeAction = imeAction,
        onImeAction = onImeAction
    )
}