package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R

/**
 * Базовое поле ввода текста с очисткой
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        if (value.isNotEmpty() && enabled && !readOnly) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.clear)
                )
            }
        }
    },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    placeholder: @Composable (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            maxLines = maxLines,
            singleLine = singleLine,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = keyboardCapitalization
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                    onImeAction()
                },
                onDone = {
                    focusManager.clearFocus()
                    onImeAction()
                },
                onSearch = {
                    focusManager.clearFocus()
                    onImeAction()
                }
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = placeholder,
            textStyle = textStyle,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer,
                cursorColor = MaterialTheme.colorScheme.primary,
                errorCursorColor = MaterialTheme.colorScheme.error,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                errorSupportingTextColor = MaterialTheme.colorScheme.error,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            visualTransformation = visualTransformation
        )

        if (isError && !errorText.isNullOrEmpty()) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

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

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSearch: () -> Unit = {},
    placeholder: String? = null
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        onImeAction = onSearch,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        placeholder = placeholder?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        }
    )
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorText = errorText,
        singleLine = true,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        trailingIcon = {
            val visibilityIcon = if (passwordVisible) {
                Icons.Default.VisibilityOff
            } else {
                Icons.Default.Visibility
            }

            val description = if (passwordVisible) {
                stringResource(id = R.string.hide_password)
            } else {
                stringResource(id = R.string.show_password)
            }

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = description
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    )
}

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