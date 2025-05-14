package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

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
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: TextUnit = TextUnit.Unspecified,
    allowDecimals: Boolean = true,
    maxDecimalPlaces: Int = 3,
    focusRequester: FocusRequester? = null
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
                onValueChange = { newText ->
                    // Удаляем все, кроме цифр, одной точки и одного минуса в начале
                    var filteredText = ""
                    var hasDecimalPoint = false
                    var hasMinusSign = false

                    for ((index, char) in newText.withIndex()) {
                        when {
                            char.isDigit() -> filteredText += char
                            char == '.' && !hasDecimalPoint -> {
                                // Разрешаем точку, только если она не первая (или после минуса)
                                if (filteredText.isNotEmpty() || (hasMinusSign && filteredText.isEmpty())) {
                                    filteredText += char
                                    hasDecimalPoint = true
                                } else if (index == 0 && newText.length > 1 && newText[1].isDigit()){
                                    // Позволяем начать с "0." если пользователь вводит ".5"
                                    filteredText = "0."
                                    hasDecimalPoint = true
                                }
                            }
                            char == '-' && index == 0 && !hasMinusSign -> {
                                filteredText += char
                                hasMinusSign = true
                            }
                        }
                    }

                    // Ограничиваем количество знаков после запятой
                    if (hasDecimalPoint) {
                        val parts = filteredText.split('.')
                        if (parts.size > 1 && parts[1].length > maxDecimalPlaces) {
                            // Обрезаем лишние знаки после запятой
                            filteredText = "${parts[0]}.${parts[1].substring(0, maxDecimalPlaces)}"
                        }
                    }
                    onValueChange(filteredText)
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
                keyboardActions = KeyboardActions(
                    onAny = {
                        if (onImeAction != null) {
                            onImeAction()
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
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
 * Вспомогательная функция для проверки валидности десятичного числа.
 * @param input Строка для проверки.
 * @param maxDecimalPlaces Максимальное количество знаков после запятой.
 * @return true, если строка является валидным десятичным числом, иначе false.
 */
fun isValidDecimal(input: String, maxDecimalPlaces: Int): Boolean {
    if (input.isEmpty()) return true // Пустое поле считаем валидным (или можно изменить логику)
    if (input == "-") return true // Разрешаем ввод одного минуса

    // Регулярное выражение для проверки:
    // ^-?                - опциональный минус в начале
    // (\\d+             - одна или более цифр (целая часть)
    // (\\.\\d{0,maxDecimalPlaces})? - опциональная десятичная часть с 0 до maxDecimalPlaces цифр
    // |                  - ИЛИ
    // \\.\\d{1,maxDecimalPlaces}) - только десятичная часть (например, ".5")
    // $                  - конец строки
    val regex = Regex("^-?((\\d+(\\.\\d{0,$maxDecimalPlaces})?)|(\\.\\d{1,$maxDecimalPlaces}))$")
    return regex.matches(input)
}

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

@Composable
fun QuantityColumn(
    label: String,
    valueSmall: String,
    valueLarge: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = valueLarge,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = "($valueSmall)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
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