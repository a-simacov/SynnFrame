package com.synngate.synnframe.presentation.common.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.theme.LocalNavigationButtonHeight
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.theme.ThemeMode

@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    badge: Int? = null,
    badgeColor: Color = MaterialTheme.colorScheme.error
) {
    val buttonHeight = LocalNavigationButtonHeight.current

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterStart)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            if (badge != null && badge > 0) {
                Badge(
                    containerColor = badgeColor,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = if (badge > 99999) "99999+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Preview(
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun NavButtonPreview() {
    SynnFrameTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            NavigationButton(
                text = stringResource(id = R.string.products),
                onClick = { },
                icon = Icons.Outlined.Inventory,
                contentDescription = stringResource(id = R.string.products),
                badge = 7536
            )
            Spacer(modifier = Modifier.height(12.dp))
            NavigationButton(
                text = stringResource(id = R.string.settings),
                onClick = { },
                icon = Icons.Default.Settings,
                contentDescription = stringResource(id = R.string.settings)
            )
            Spacer(modifier = Modifier.height(12.dp))
            NavigationButton(
                text = stringResource(id = R.string.sync_data),
                onClick = { },
                contentDescription = null
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    buttonHeight: Float = 72f,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = buttonColors
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ActionButtonPreview() {
    SynnFrameTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ActionButton(
                text = stringResource(id = R.string.sync_data),
                onClick = { },
                icon = Icons.Default.Sync,
                contentDescription = stringResource(id = R.string.sync_data),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                text = stringResource(id = R.string.login),
                onClick = { },
                isLoading = false,
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                text = stringResource(id = R.string.save),
                onClick = { },
                enabled = true,
                isLoading = false,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Кнопка для вторичных действий
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonHeight: Float = 72f,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Кнопка для отмены/назад
 */
@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Float = 72f,
    text: String = stringResource(R.string.back),
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Кнопка изменения свойства - при нажатии изменяется свойство объекта
 */
@Composable
fun PropertyToggleButton(
    property: String,
    value: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonHeight: Float = 72f
) {
    val valueText = if (value) {
        stringResource(id = R.string.yes)
    } else {
        stringResource(id = R.string.no)
    }

    OutlinedButton(
        onClick = { onToggle(!value) },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "$property: $valueText",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Кнопка с выделением (для меню или выбора элемента)
 */
@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Float = 72f,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    val buttonColors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    val buttonModifier = if (isSelected) {
        modifier
            .fillMaxWidth()
            .height(buttonHeight.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(buttonHeight.dp)
    }

    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = PaddingValues(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = buttonColors
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = PaddingValues(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Универсальная циклическая кнопка для переключения между значениями.
 *
 * @param T тип значений для переключения
 * @param values список всех возможных значений
 * @param currentValue текущее выбранное значение
 * @param onValueChange колбэк, вызываемый при изменении значения
 * @param valueToString функция для преобразования значения в строку для отображения
 * @param labelText опциональный текст метки, который будет отображаться перед значением
 * @param buttonHeight высота кнопки в dp
 * @param modifier модификатор для стилизации кнопки
 */
@Composable
fun <T> CyclicValueButton(
    values: List<T>,
    currentValue: T,
    onValueChange: (T) -> Unit,
    valueToString: @Composable (T) -> String,
    labelText: String? = null,
    buttonHeight: Float = 72f,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        return
    }

    val valueText = valueToString(currentValue)
    val buttonText = if (labelText != null) "$labelText: $valueText" else valueText

    val onClick = {
        val currentIndex = values.indexOf(currentValue)
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % values.size
        onValueChange(values[nextIndex])
    }

    ActionButton(
        text = buttonText,
        onClick = onClick,
        modifier = modifier,
        buttonHeight = buttonHeight
    )
}

@Composable
fun <T> CarouselValueButton(
    values: List<T>,
    currentValue: T,
    onValueChange: (T) -> Unit,
    valueToString: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    buttonHeight: Dp = 72.dp,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors()
) {
    if (values.isEmpty()) {
        return
    }

    val currentIndex = values.indexOf(currentValue)
    if (currentIndex == -1) {
        onValueChange(values.firstOrNull() ?: return)
        return
    }

    val prevIndex = if (currentIndex == 0) values.lastIndex else currentIndex - 1
    val nextIndex = (currentIndex + 1) % values.size

    // Получаем строковые представления значений
    val prevValueText = if (values.size > 2) valueToString(values[prevIndex]) else ""
    val currentValueText = valueToString(currentValue)
    val nextValueText = valueToString(values[nextIndex])

    Button(
        onClick = {
            onValueChange(values[nextIndex])
        },
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        contentPadding = PaddingValues(8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = buttonColors
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (labelText != null) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = prevValueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.25f),
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentValueText,
                        style = MaterialTheme.typography.titleLarge,
                        //modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующее",
                        modifier = Modifier.size(16.dp),
                        tint = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = nextValueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.25f)
                )
            }
        }
    }
}

@Preview(
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun CarouselValueButtonPreview() {
    SynnFrameTheme {
        Column {
            CarouselValueButton(
                values = ThemeMode.entries,
                currentValue = ThemeMode.SYSTEM,
                onValueChange = {},
                valueToString = { theme ->
                    when (theme) {
                        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
                        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                        ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                    }
                },
                //labelText = stringResource(id = R.string.theme),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            CarouselValueButton(
                values = listOf(true, false),
                currentValue = false,
                onValueChange = {},
                valueToString = { value ->
                    when (value) {
                        false -> "НЕТ"
                        true -> "ДА"
                    }
                },
                //labelText = stringResource(id = R.string.theme),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}