package com.synngate.synnframe.presentation.ui.wizard.action.components

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

/**
 * Унифицированный компонент ввода для шагов визарда
 *
 * @param value Текущее значение поля
 * @param onValueChange Обработчик изменения значения
 * @param label Метка поля ввода
 * @param modifier Модификатор
 * @param isError Флаг ошибки
 * @param errorText Текст ошибки
 * @param keyboardType Тип клавиатуры
 * @param imeAction Действие клавиатуры
 * @param onImeAction Обработчик действия клавиатуры
 * @param leadingIcon Иконка в начале поля
 * @param trailingIcon Иконка в конце поля
 * @param enabled Доступность поля для редактирования
 * @param singleLine Однострочный режим
 * @param placeholder Подсказка в поле
 */
@Composable
fun WizardInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    placeholder: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = placeholder?.let { { Text(it) } }
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

/**
 * Поле ввода со сканером штрих-кода
 *
 * @param value Текущее значение
 * @param onValueChange Обработчик изменения значения
 * @param onSearch Обработчик поиска
 * @param onScannerClick Обработчик нажатия на кнопку сканера
 * @param modifier Модификатор
 * @param label Метка поля
 * @param isError Флаг ошибки
 * @param errorText Текст ошибки
 * @param onSelectFromList Обработчик выбора из списка (null, если выбор из списка не доступен)
 * @param placeholder Подсказка в поле
 */
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
    placeholder: String? = null
) {
    WizardInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        isError = isError,
        errorText = errorText,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        onImeAction = onSearch,
        placeholder = placeholder,
        leadingIcon = if (onSelectFromList != null) {
            {
                IconButton(onClick = onSelectFromList) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
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
        }
    )
}

/**
 * Вспомогательный компонент для создания отступа между элементами формы
 */
@Composable
fun FormSpacer(height: Int = 16) {
    Spacer(modifier = Modifier.height(height.dp))
}