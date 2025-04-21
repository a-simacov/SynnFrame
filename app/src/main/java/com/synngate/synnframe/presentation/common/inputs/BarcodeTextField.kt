package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun BarcodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    trailingIcon: @Composable (() -> Unit)
) {
    AppTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)

            // Проверяем, был ли введен штрихкод с помощью сканера
            // Обычно сканеры добавляют символ переноса строки в конце
            if (newValue.endsWith("\n") || newValue.endsWith("\r")) {
                val barcode = newValue.trim()
                onBarcodeScanned(barcode)

                // Очищаем поле ввода после сканирования
                onValueChange("")
            }
        },
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        onImeAction = {
            if (value.isNotEmpty()) {
                onBarcodeScanned(value)
                onValueChange("")
            }
        },
        trailingIcon = trailingIcon
    )
}