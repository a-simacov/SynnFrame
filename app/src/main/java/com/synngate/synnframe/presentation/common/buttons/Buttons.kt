package com.synngate.synnframe.presentation.common.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.theme.LocalNavigationButtonHeight

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

@Composable
fun BooleanButton(
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    valueToString: @Composable (Boolean) -> String = { stringResource(id = if (it) R.string.yes else R.string.no) },
    labelText: String = ""
) {
    if (labelText.isNotBlank())
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

    CarouselValueButton(
        values = listOf(false, true),
        currentValue = currentValue,
        onValueChange = onValueChange,
        valueToString = valueToString,
        modifier = modifier
    )
}