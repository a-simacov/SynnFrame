package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Компонент для ввода количества с кнопками + и -
 *
 * @param value Текущее значение
 * @param onValueChange Обработчик изменения значения
 * @param modifier Модификатор
 * @param label Метка поля ввода
 * @param isError Флаг ошибки
 * @param errorText Текст ошибки
 * @param onIncrement Обработчик увеличения значения
 * @param onDecrement Обработчик уменьшения значения
 * @param enabled Флаг доступности компонента
 * @param textAlign Выравнивание текста в поле ввода
 * @param fontSize Размер шрифта в поле ввода
 */
@Composable
fun QuantityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Количество",
    isError: Boolean = false,
    errorText: String? = null,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onDecrement != null) {
                IconButton(
                    onClick = onDecrement,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Уменьшить",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Фильтруем ввод, чтобы были только цифры и точка/запятая
                    val filteredValue = newValue.replace(",", ".").filter {
                        it.isDigit() || it == '.'
                    }
                    onValueChange(filteredValue)
                },
                label = if (label.isNotEmpty()) {
                    { Text(label) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions.Default,
                modifier = Modifier.weight(1f),
                isError = isError,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = textAlign,
                    fontSize = if (fontSize != TextUnit.Unspecified) fontSize else MaterialTheme.typography.bodyLarge.fontSize
                ),
                colors = TextFieldDefaults.colors(
                    // Устанавливаем цвета для поля ввода
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
            )

            if (onIncrement != null) {
                IconButton(
                    onClick = onIncrement,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Увеличить",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        if (isError && errorText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Ошибка",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 4.dp)
                )
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
fun QuantityRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    warning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (warning)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}