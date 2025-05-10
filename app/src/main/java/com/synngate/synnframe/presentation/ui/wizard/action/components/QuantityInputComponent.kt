package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Кнопка управления количеством с поддержкой долгого нажатия
 */
@Composable
private fun QuantityControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var job: Job? = null

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            // Сразу выполняем первое нажатие
                            onClick()

                            isPressed = true

                            // Запускаем корутину для повторяющихся нажатий
                            job = coroutineScope.launch {
                                // Ждем перед началом повторений
                                delay(300)

                                while (isPressed) {
                                    onClick()
                                    // Задержка между повторениями
                                    delay(100)
                                }
                            }

                            // Ждем, пока пользователь отпустит палец
                            val released = tryAwaitRelease()
                            isPressed = false
                            job?.cancel()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
    }
}

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
 * @param onClear Обработчик очистки поля
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
    onClear: (() -> Unit)? = null,
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
                QuantityControlButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Уменьшить",
                    onClick = onDecrement,
                    enabled = enabled
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Заменяем запятую на точку
                    val withDot = newValue.replace(",", ".")

                    // Фильтруем, оставляя только цифры и точку
                    val filtered = withDot.filter { char ->
                        char.isDigit() || char == '.'
                    }

                    // Обрабатываем особые случаи
                    val processed = when {
                        // Если пустая строка - передаем как есть (обработается в ViewModel)
                        filtered.isEmpty() -> ""
                        // Если только точка - разрешаем
                        filtered == "." -> "."
                        // Если более одной точки - оставляем только первую
                        filtered.count { it == '.' } > 1 -> {
                            val firstDotIndex = filtered.indexOf('.')
                            filtered.substring(0, firstDotIndex + 1) +
                                    filtered.substring(firstDotIndex + 1).replace(".", "")
                        }
                        else -> filtered
                    }

                    // Ограничиваем количество знаков после запятой до 3
                    val finalValue = if (processed.contains('.')) {
                        val parts = processed.split('.')
                        if (parts.size > 1 && parts[1].length > 3) {
                            "${parts[0]}.${parts[1].substring(0, 3)}"
                        } else {
                            processed
                        }
                    } else {
                        processed
                    }

                    onValueChange(finalValue)
                },
                label = if (label.isNotEmpty()) {
                    { Text(label) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal, // Изменено с Number на Decimal
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
                trailingIcon = if (onClear != null && value.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = onClear,
                            enabled = enabled
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else null
            )

            if (onIncrement != null) {
                QuantityControlButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Увеличить",
                    onClick = onIncrement,
                    enabled = enabled
                )
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