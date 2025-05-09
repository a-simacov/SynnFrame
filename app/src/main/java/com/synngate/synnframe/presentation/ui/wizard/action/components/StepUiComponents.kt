package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import timber.log.Timber

@Composable
fun StepTitle(
    step: ActionStep,
    action: PlannedAction,
    modifier: Modifier = Modifier
) {
    Text(
        text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(
                MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Ошибка валидации",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Загрузка...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun BarcodeEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Введите штрихкод",
    isError: Boolean = false,
    errorText: String? = null,
    onSelectFromList: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                Timber.d("BarcodeEntryField: value changed from '$value' to '$newValue'")
                // Если ввели "0" и есть обработчик выбора из списка
                if (newValue == "0" && onSelectFromList != null) {
                    onSelectFromList()
                    onValueChange("") // Очищаем поле
                } else {
                    onValueChange(newValue)
                }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            leadingIcon = if (onSelectFromList != null) {
                {
                    IconButton(onClick = onSelectFromList) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Выбрать из списка"
                        )
                    }
                }
            } else null,
            trailingIcon = {
                IconButton(onClick = onScannerClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Сканировать"
                    )
                }
            },
            isError = isError
        )

        if (isError && errorText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Ошибка",
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