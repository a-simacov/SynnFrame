package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import timber.log.Timber

@Composable
fun WizardBarcodeField(
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
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (value.isNotEmpty()) {
                        Timber.d("Нажата клавиша IME Search на клавиатуре: $value")
                        onSearch()
                    }
                }
            ),
            leadingIcon = if (onSelectFromList != null) {
                {
                    IconButton(onClick = onSelectFromList) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null
                        )
                    }
                }
            } else null,
            trailingIcon = {
                IconButton(onClick = onScannerClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                }
            },
            placeholder = placeholder?.let { { Text(it) } }
        )

        if (isError && errorText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FormSpacer(height: Int = 16) {
    Spacer(modifier = Modifier.height(height.dp))
}