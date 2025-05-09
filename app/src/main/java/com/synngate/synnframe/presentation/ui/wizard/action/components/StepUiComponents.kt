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

/**
 * Отображает заголовок шага с описанием действия
 */
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

/**
 * Отображает сообщение об ошибке
 */
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

/**
 * Отображает индикатор загрузки
 */
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

/**
 * Поле для ввода штрих-кода с кнопкой сканирования
 */
@Composable
fun BarcodeEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Введите штрихкод",
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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