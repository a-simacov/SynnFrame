package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Улучшенный компонент для ввода количества с возможностью инкремента/декремента
 * и поддержкой десятичных значений
 */
@Composable
fun WizardQuantityInput(
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
    fontSize: TextUnit = TextUnit.Unspecified,
    allowDecimals: Boolean = true
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

                    // Применяем фильтрацию в зависимости от того, разрешены ли десятичные
                    val filtered = if (allowDecimals) {
                        // Фильтруем, оставляя только цифры и точку
                        withDot.filter { char ->
                            char.isDigit() || char == '.'
                        }
                    } else {
                        // Оставляем только цифры
                        withDot.filter { it.isDigit() }
                    }

                    // Обрабатываем особые случаи для десятичных чисел
                    val processed = if (allowDecimals) {
                        when {
                            // Если пустая строка - передаем как есть
                            filtered.isEmpty() -> ""
                            // Если только точка - разрешаем
                            filtered == "." -> "0."
                            // Если более одной точки - оставляем только первую
                            filtered.count { it == '.' } > 1 -> {
                                val firstDotIndex = filtered.indexOf('.')
                                filtered.substring(0, firstDotIndex + 1) +
                                        filtered.substring(firstDotIndex + 1).replace(".", "")
                            }
                            else -> filtered
                        }
                    } else {
                        filtered
                    }

                    // Ограничиваем количество знаков после запятой до 3
                    val finalValue = if (allowDecimals && processed.contains('.')) {
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
                    keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
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
 * Компонент для отображения количественных показателей
 */
@Composable
fun QuantityIndicatorCard(
    title: String,
    planned: Float,
    completed: Float,
    current: Float = 0f,
    modifier: Modifier = Modifier
) {
    val total = completed + current
    val remaining = (planned - total).coerceAtLeast(0f)
    val isOverLimit = planned > 0f && total > planned

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Строка с плановым количеством
            QuantityRow(
                label = "План:",
                value = formatQuantityDisplay(planned)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Строка с выполненным количеством
            QuantityRow(
                label = "Выполнено:",
                value = formatQuantityDisplay(completed)
            )

            if (current > 0f) {
                Spacer(modifier = Modifier.height(4.dp))

                // Строка с текущим вводимым количеством
                QuantityRow(
                    label = "Текущее:",
                    value = formatQuantityDisplay(current)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Строка с итоговым количеством
                QuantityRow(
                    label = "Итого будет:",
                    value = formatQuantityDisplay(total),
                    highlight = true,
                    warning = isOverLimit
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Строка с оставшимся количеством
            QuantityRow(
                label = "Осталось:",
                value = formatQuantityDisplay(remaining),
                warning = isOverLimit
            )

            if (isOverLimit) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Внимание: превышение планового количества!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Компонент для отображения строки с меткой и значением
 */
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

/**
 * Форматирует количество для отображения с округлением до 3 знаков после запятой
 */
fun formatQuantityDisplay(value: Float): String {
    return if (value % 1f == 0f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    }
}