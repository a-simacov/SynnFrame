package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import kotlin.math.absoluteValue

/**
 * Карточка для отображения планового действия с обновленным дизайном
 * С адаптивной шириной блока количества
 */
@Composable
fun PlannedActionCard(
    actionUI: PlannedActionUI,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val action = actionUI.action

    // Определяем цвет левой полосы статуса
    val statusBarColor = when {
        actionUI.isInitialAction -> MaterialTheme.colorScheme.primary
        actionUI.isFinalAction -> MaterialTheme.colorScheme.tertiary
        actionUI.isCompleted -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    // Фон элемента
    val backgroundColor = if (actionUI.isCompleted)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surface

    // Информация о количестве
    val hasQuantity = actionUI.quantity > 0f
    val quantityText: String
    val diffText: String
    val quantityColor: Color

    if (hasQuantity) {
        if (actionUI.completedQuantity == 0f) {
            // Только план
            quantityText = formatQuantity(actionUI.quantity)
            diffText = ""
            quantityColor = MaterialTheme.colorScheme.outline
        } else if (actionUI.completedQuantity == actionUI.quantity) {
            // План совпадает с фактом
            quantityText = formatQuantity(actionUI.quantity)
            diffText = ""
            quantityColor = MaterialTheme.colorScheme.secondary
        } else {
            // План не совпадает с фактом
            quantityText = formatQuantity(actionUI.completedQuantity)
            val diff = actionUI.quantity - actionUI.completedQuantity
            val sign = if (diff > 0) "+" else ""
            diffText = "($sign${formatQuantity(diff.absoluteValue)})"
            quantityColor = if (diff > 0)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        }
    } else {
        quantityText = ""
        diffText = ""
        quantityColor = Color.Unspecified
    }

    // Прогресс по количеству
    val progressPercentage = if (hasQuantity && actionUI.quantity > 0f)
        (actionUI.completedQuantity / actionUI.quantity) * 100f
    else
        0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = actionUI.isClickable) { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (actionUI.isCompleted) 1.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Левая полоса статуса
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusBarColor)
            )

            // Основное содержимое
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(8.dp)
            ) {
                // Информация о товаре (выделено)
                if (action.storageProduct != null || action.storageProductClassifier != null) {
                    val productName = action.storageProduct?.product?.name
                        ?: action.storageProductClassifier?.name
                        ?: "Неизвестный товар"

                    Text(
                        text = productName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Информация о ячейках
                if (action.storageBin != null || action.placementBin != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        action.storageBin?.let {
                            Text(
                                text = "Из: ${it.code}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        action.placementBin?.let {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "В: ${it.code}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Информация о паллетах
                if (action.storagePallet != null || action.placementPallet != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        action.storagePallet?.let {
                            Text(
                                text = "Паллета: ${it.code}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        action.placementPallet?.let {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "На паллету: ${it.code}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Гибкий пробел, чтобы название было внизу
                Spacer(modifier = Modifier.weight(1f, fill = true))

                // Название действия с номером (внизу)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Номер действия
                    Text(
                        text = "#${actionUI.order}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Название шаблона действия
                    Text(
                        text = actionUI.name,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Блок количества (справа) - с адаптивной шириной
            if (hasQuantity) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    modifier = Modifier
                        .widthIn(min = 48.dp, max = 80.dp) // Минимальная и максимальная ширина
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = quantityText,
                        color = quantityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (diffText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = diffText,
                            color = quantityColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Правая полоса прогресса
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(progressPercentage / 100f)
                            .background(
                                if (progressPercentage >= 100f)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/**
 * Форматирует числовое значение для отображения.
 * - Для целых чисел отображает без десятичной части
 * - Для дробных чисел отображает до 3 знаков после запятой
 * - Для больших чисел использует сокращенный формат
 */
private fun formatQuantity(value: Float): String {
    return when {
        // Целое число
        value % 1f == 0f -> {
            val intValue = value.toInt()
            when {
                intValue < 1000 -> intValue.toString()
                intValue < 10000 -> String.format("%.1fK", intValue / 1000f).replace(".0K", "K")
                intValue < 1000000 -> String.format("%dK", intValue / 1000)
                else -> String.format("%.1fM", intValue / 1000000f).replace(".0M", "M")
            }
        }

        // Десятичное число с обрезкой лишних нулей
        else -> {
            val formattedValue = when {
                value < 0.01f -> String.format("%.3f", value)
                value < 0.1f -> String.format("%.2f", value)
                value < 1000f -> String.format("%.1f", value)
                value < 10000f -> String.format("%.1fK", value / 1000f)
                value < 1000000f -> String.format("%dK", (value / 1000f).toInt())
                else -> String.format("%.1fM", value / 1000000f)
            }

            // Удаляем лишние нули после запятой
            formattedValue.replace("\\.0+([KM]?)$".toRegex(), "$1")
        }
    }
}