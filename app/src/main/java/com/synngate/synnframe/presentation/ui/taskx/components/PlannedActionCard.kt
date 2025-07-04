package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import kotlin.math.absoluteValue

/**
 * Карточка действия, разворачивающаяся при долгом нажатии
 */
@Composable
fun ExpandableActionCard(
    actionUI: PlannedActionUI,
    onClick: () -> Unit,
    onToggleStatus: (PlannedActionUI, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Состояние развернутости карточки
    var isExpanded by remember { mutableStateOf(false) }

    // Для тактильной обратной связи
    val hapticFeedback = LocalHapticFeedback.current

    // Проверяем, можно ли изменять статус вручную
    val canToggleStatus = actionUI.canBeCompletedManually || actionUI.manuallyCompleted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f), // Очень светлая граница
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(actionUI.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            if (canToggleStatus) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isExpanded = !isExpanded
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Без тени
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            // Вставляем содержимое PlannedActionCard, но без его собственных обработчиков
            PlannedActionCardContent(actionUI)
        }

        // Раскрывающаяся часть с действиями
        AnimatedVisibility(
            visible = isExpanded && canToggleStatus,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Divider()

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (actionUI.manuallyCompleted) {
                        // Кнопка для снятия отметки
                        OutlinedButton(
                            onClick = {
                                onToggleStatus(actionUI, false)
                                isExpanded = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Mark")
                        }
                    } else if (actionUI.canBeCompletedManually) {
                        // Кнопка для отметки о выполнении
                        Button(
                            onClick = {
                                onToggleStatus(actionUI, true)
                                isExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark as Completed")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Внутреннее содержимое карточки без собственных обработчиков нажатий
 * Извлечено из PlannedActionCard, но без модификатора clickable
 */
@Composable
private fun PlannedActionCardContent(actionUI: PlannedActionUI) {
    val action = actionUI.action

    // Определяем яркие, контрастные цвета для индикаторов
    val statusBarColor = when {
        actionUI.isInitialAction -> Color(0xFF1976D2) // Яркий синий
        actionUI.isFinalAction -> Color(0xFFC2185B)   // Яркий пурпурный
        actionUI.isCompleted -> Color(0xFF388E3C)     // Яркий зеленый
        else -> Color(0xFF757575)                     // Серый
    }

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
            quantityColor = Color(0xFF388E3C)  // Яркий зеленый
        } else {
            // План не совпадает с фактом
            quantityText = formatQuantity(actionUI.completedQuantity)
            val diff = actionUI.quantity - actionUI.completedQuantity
            val sign = if (diff > 0) "+" else ""
            diffText = "($sign${formatQuantity(diff.absoluteValue)})"
            quantityColor = if (diff > 0)
                Color(0xFF1976D2)  // Яркий синий
            else
                Color(0xFFD32F2F)  // Яркий красный
        }
    } else {
        quantityText = ""
        diffText = ""
        quantityColor = Color.Unspecified
    }

    // Прогресс по количеству
    val progressPercentage = if (hasQuantity && actionUI.quantity > 0f)
        (actionUI.completedQuantity / actionUI.quantity) * 100f
    else if (actionUI.isCompleted)
        100f // Если действие выполнено, но нет количества, показываем полную полосу
    else
        0f

    // Контрастные цвета для индикатора прогресса
    val progressBackgroundColor = Color(0xFFE0E0E0)  // Светло-серый, хорошо видный в обеих темах
    val progressFillColor = if (progressPercentage >= 100f)
        Color(0xFF388E3C)  // Яркий зеленый
    else
        Color(0xFF1976D2)  // Яркий синий

    // Определяем, есть ли запланированные объекты
    val hasPlannedObjects = action.storageProduct != null ||
            action.storageProductClassifier != null ||
            action.storageBin != null ||
            action.placementBin != null ||
            action.storagePallet != null ||
            action.placementPallet != null

    val actionWithoutPlannedObjects = (actionUI.isInitialAction || actionUI.isFinalAction) && !hasPlannedObjects
    // Определяем размер шрифта для названия шаблона
    val templateNameFontSize = if (actionWithoutPlannedObjects) {
        16.sp // Увеличенный размер для начальных/финальных действий без объектов
    } else {
        8.sp // Стандартный размер
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // ЛЕВАЯ ПОЛОСА СТАТУСА
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(statusBarColor)
        )

        // Основное содержимое в колонке с отступами
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp)
        ) {
            // Информация о товаре
            if (action.storageProduct != null || action.storageProductClassifier != null) {
                val productName = action.storageProduct?.product?.name
                    ?: action.storageProductClassifier?.name
                    ?: "Unknown product"

                Text(
                    text = productName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Информация о ячейках
            if (action.storageBin != null || action.placementBin != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    action.storageBin?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF388E3C), // Зелёный для ячейки хранения
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF388E3C), // Зелёный для ячейки хранения
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it.code,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF388E3C) // Зелёный цвет текста для ячейки хранения
                            )
                        }
                    }

                    action.placementBin?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1976D2), // Синий для ячейки размещения
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF1976D2), // Синий для ячейки размещения
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it.code,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2) // Синий цвет текста для ячейки размещения
                            )
                        }
                    }
                }
            }

            // Информация о паллетах
            if (action.storagePallet != null || action.placementPallet != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    action.storagePallet?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color(0xFF388E3C), // Зелёный для паллеты хранения
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF388E3C), // Зелёный для паллеты хранения
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it.code,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF388E3C) // Зелёный цвет текста для паллеты хранения
                            )
                        }
                    }

                    action.placementPallet?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color(0xFF1976D2), // Синий для паллеты размещения
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF1976D2), // Синий для паллеты размещения
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it.code,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2) // Синий цвет текста для паллеты размещения
                            )
                        }
                    }
                }
            }

            // Гибкий пробел, чтобы название было внизу
            Spacer(modifier = Modifier.weight(1f, fill = true))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Номер действия - размер не изменяется
                Text(
                    text = "#${actionUI.order}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // Название шаблона действия - размер может изменяться
                Text(
                    text = actionUI.name,
                    fontSize = templateNameFontSize,
                    fontWeight = if (actionWithoutPlannedObjects)
                        FontWeight.Bold else FontWeight.Normal,
                    color = if (actionWithoutPlannedObjects)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    maxLines = if (actionWithoutPlannedObjects) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Индикатор ручного выполнения
                if (actionUI.manuallyCompleted) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        color = Color(0xFF388E3C),
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                    )
                }
            }
        }

        // Блок количества (справа) - отображается, только если есть количество
        if (hasQuantity) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = quantityText,
                    color = quantityColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (diffText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = diffText,
                        color = quantityColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ПРАВАЯ ПОЛОСА ПРОГРЕССА
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(progressBackgroundColor)
        ) {
            if (progressPercentage > 0) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(progressPercentage / 100f)
                        .background(progressFillColor)
                        .align(Alignment.BottomCenter)
                )
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
                intValue < 10000 -> intValue.toString() // Показываем как есть до 10_000
                intValue < 1000000 -> String.format("%dK", intValue / 1000)
                else -> String.format("%.1fM", intValue / 1000000f).replace(".0M", "M")
            }
        }

        // Десятичное число с обрезкой лишних нулей
        else -> {
            val formattedValue = when {
                value < 0.01f -> String.format("%.3f", value)
                value < 0.1f -> String.format("%.2f", value)
                value < 10000f -> String.format("%.1f", value) // Показываем как есть до 10_000
                value < 1000000f -> String.format("%dK", (value / 1000f).toInt())
                else -> String.format("%.1fM", value / 1000000f)
            }

            // Удаляем лишние нули после запятой
            formattedValue.replace("\\.0+([KM]?)$".toRegex(), "$1")
        }
    }
}

@Preview(apiLevel = 34, showBackground = true)
@Composable
private fun PlannedActionCardPreview() {
    SynnFrameTheme {
        Column {
            ExpandableActionCard(
                actionUI = PlannedActionUI(
                    PlannedAction(
                        id = "",
                        order = 2,
                        actionTemplateId = "",
                        actionTemplate = null,
                        completionOrderType = CompletionOrderType.REGULAR,
                        storageProductClassifier = Product(
                            id = "",
                            name = "Ночник 3D MOON LAMP CompletionOrderType.REGULAR storageProductClassifier"
                        ),
                        storageBin = BinX(code = "01I-07-001-1", zone = ""),
                        placementBin = BinX(code = "01A-01-002-2", zone = ""),
                        storagePallet = Pallet("IN00000000009"),
                        placementPallet = Pallet("IN00000000010"),
                        isCompleted = true,
                        quantity = 2000f
                    ),
                    isCompleted = true,
                    completedQuantity = 1223f
                ),
                onClick = { /*TODO*/ },
                onToggleStatus = { _, _ -> }
            )
            ExpandableActionCard(
                actionUI = PlannedActionUI(
                    PlannedAction(
                        id = "",
                        order = 3,
                        actionTemplateId = "",
                        actionTemplate = null,
                        completionOrderType = CompletionOrderType.FINAL,
                        isCompleted = true,
                    ),
                    isCompleted = true,
                    completedQuantity = 1223f
                ),
                onClick = { /*TODO*/ },
                onToggleStatus = { _, _ -> }
            )
        }
    }
}