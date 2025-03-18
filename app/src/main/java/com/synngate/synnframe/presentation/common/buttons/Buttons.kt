package com.synngate.synnframe.presentation.common.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Кнопка навигации - при нажатии выполняется переход на другой экран
 */
@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonHeight: Float = 72f,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    badge: Int? = null,
    badgeColor: Color = MaterialTheme.colorScheme.error
) {
    Button(
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

            if (badge != null && badge > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Badge(
                    containerColor = badgeColor
                ) {
                    Text(
                        text = if (badge > 99) "99+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

/**
 * Кнопка действия - при нажатии выполняется действие,
 * может содержать индикатор прогресса
 */
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
    buttonColors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight.dp),
        contentPadding = PaddingValues(16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = buttonColors
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
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
 * Кнопка, которая изменяет свое значение в зависимости от настройки из Flow
 */
@Composable
fun SettingToggleButton(
    settingName: String,
    settingValue: Flow<Boolean>,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonHeight: Float = 72f
) {
    var value by remember { mutableStateOf(false) }

    // Загружаем значение из Flow один раз при композиции
    LaunchedEffect(settingValue) {
        value = settingValue.firstOrNull() ?: false
    }

    PropertyToggleButton(
        property = settingName,
        value = value,
        onToggle = {
            value = it
            onToggle(it)
        },
        modifier = modifier,
        enabled = enabled,
        buttonHeight = buttonHeight
    )
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