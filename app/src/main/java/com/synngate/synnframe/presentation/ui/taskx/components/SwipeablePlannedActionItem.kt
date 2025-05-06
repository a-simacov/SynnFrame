package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Announcement
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Компонент для отображения запланированного действия с поддержкой свайпа для изменения статуса
 */
@Composable
fun SwipeablePlannedActionItem(
    action: PlannedAction,
    factActions: List<FactAction>,
    onClick: () -> Unit,
    onToggleCompletion: ((Boolean) -> Unit)? = null,
    isNextAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Определяем цвет фона и информацию о статусе действия
    // Определяем цвет фона и информацию о статусе действия
    // Кэшируем результат isActionCompleted и пересчитываем только
    // когда меняются данные, влияющие на результат
    val isCompleted = remember(
        action.id,
        action.isCompleted,
        action.manuallyCompleted,
        factActions.size,
        // Хэш-сумма для отслеживания изменений в количестве
        factActions.filter { it.plannedActionId == action.id }
            .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
    ) {
        action.isActionCompleted(factActions)
    }

    // Кэшируем прогресс, чтобы не пересчитывать его при каждой перерисовке
    val progress = remember(
        action.id,
        isCompleted,
        factActions.size,
        factActions.filter { it.plannedActionId == action.id }
            .sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
    ) {
        action.calculateProgress(factActions)
    }
    val hasMultipleFactActions = action.canHaveMultipleFactActions()

    // Определяем, было ли действие завершено вручную
    val isManuallyCompleted = action.manuallyCompleted

    // Рассчитываем плановое и фактическое количество для отображения
    val plannedQuantity = action.getPlannedQuantity()
    val completedQuantity = action.getCompletedQuantity(factActions)

    // Цвет фона в зависимости от состояния
    val backgroundColor = when {
        isCompleted -> MaterialTheme.colorScheme.secondaryContainer
        action.isSkipped -> MaterialTheme.colorScheme.tertiaryContainer
        action.isFinalAction -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        isNextAction -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    // Определяем иконку и описание статуса
    val (statusIcon, statusDescription) = when {
        isCompleted && isManuallyCompleted -> Pair(
            Icons.Default.CheckCircle,
            "Завершено вручную"
        )
        isCompleted -> Pair(Icons.Default.CheckCircle, "Выполнено")
        action.isSkipped -> Pair(Icons.Default.NoEncryption, "Пропущено")
        else -> Pair(Icons.Default.PendingActions, "Ожидает выполнения")
    }

    // Определяем цвет прогресса и иконок
    val progressColor = when {
        isCompleted -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    // Border для карточки если это следующее действие
    val cardBorder = if (isNextAction && !isCompleted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    // Состояние для свайпа
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 200f // Порог для активации действия
    var showActionButtons by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Фон для свайпа (отображается под основной карточкой)
        if (onToggleCompletion != null && !action.isSkipped && hasMultipleFactActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isCompleted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая кнопка (удалить отметку)
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Отметить как невыполненное",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Отметить как невыполненное",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Правая кнопка (отметить как выполненное)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Отметить как выполненное",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Отметить как выполненное",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Основная карточка с возможностью свайпа
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(action.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Проверяем, достигнут ли порог для активации
                            if (onToggleCompletion != null &&
                                !action.isSkipped &&
                                hasMultipleFactActions) {

                                if (offsetX <= -swipeThreshold && !isCompleted) {
                                    // Левый свайп для неотмеченного -> отметить как выполненное
                                    onToggleCompletion(true)
                                } else if (offsetX >= swipeThreshold && isCompleted) {
                                    // Правый свайп для отмеченного -> снять отметку
                                    onToggleCompletion(false)
                                }
                            }

                            // Возвращаем карточку в исходное положение
                            offsetX = 0f
                            showActionButtons = false
                        },
                        onDragCancel = {
                            // Возвращаем в исходное положение при отмене
                            offsetX = 0f
                            showActionButtons = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Ограничиваем свайп только для действий с поддержкой управления статусом
                            if (onToggleCompletion != null &&
                                !action.isSkipped &&
                                hasMultipleFactActions) {

                                // Определяем направления свайпа в зависимости от статуса
                                // Для завершенных - только вправо (снять отметку)
                                // Для незавершенных - только влево (отметить)
                                val newOffset = offsetX + dragAmount
                                offsetX = when {
                                    isCompleted && newOffset > 0 -> newOffset.coerceIn(0f, 300f)
                                    !isCompleted && newOffset < 0 -> newOffset.coerceIn(-300f, 0f)
                                    else -> offsetX // Не изменяем, если свайп в неправильном направлении
                                }

                                // Показываем кнопки действий, если смещение достаточно большое
                                showActionButtons = offsetX.absoluteValue >= 75f
                            }
                        }
                    )
                }
                .clickable(onClick = onClick, enabled = action.isClickable()),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Верхняя часть карточки с названием и статусом
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Основная информация о действии
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (action.isFinalAction) {
                                IconWithTooltip(
                                    icon = Icons.AutoMirrored.Outlined.Announcement,
                                    description = "Финальное действие",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            if (isNextAction && !isCompleted) {
                                IconWithTooltip(
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    description = "Следующее действие",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Название действия
                            Text(
                                text = action.actionTemplate.name,
                                style = MaterialTheme.typography.titleMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }

                    // Иконка статуса с подсказкой
                    IconWithTooltip(
                        icon = statusIcon,
                        description = statusDescription,
                        tint = progressColor,
                        iconSize = 24
                    )
                }

                // Дополнительная информация о товаре, если есть
                action.storageProduct?.let { product ->
                    Text(
                        text = "Товар: ${product.product.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Показываем количество для действий с учетом количества
                    if (hasMultipleFactActions && plannedQuantity > 0) {
                        Text(
                            text = "Количество: $completedQuantity / $plannedQuantity",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Отображение паллеты хранения, если есть
                action.storagePallet?.let { pallet ->
                    Text(
                        text = "Паллета хранения: ${pallet.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Информация о типе действия
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    WmsActionIconWithTooltip(
                        wmsAction = action.wmsAction,
                        iconSize = 18
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getWmsActionDescription(action.wmsAction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Отображение ячейки размещения, если есть
                action.placementBin?.let { bin ->
                    Text(
                        text = "Ячейка размещения: ${bin.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Отображение паллеты размещения, если есть
                action.placementPallet?.let { pallet ->
                    Text(
                        text = "Паллета размещения: ${pallet.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Индикатор прогресса для действий с учетом количества
                if (hasMultipleFactActions && !action.isSkipped) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Прогресс-бар
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = progressColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Количество фактических действий
                    val relatedFacts = factActions.filter { it.plannedActionId == action.id }
                    if (relatedFacts.isNotEmpty()) {
                        Text(
                            text = "Выполнено действий: ${relatedFacts.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }

                // Индикаторы свайпа
                if (onToggleCompletion != null && !action.isSkipped && hasMultipleFactActions) {
                    AnimatedVisibility(
                        visible = showActionButtons,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = if (isCompleted)
                                Arrangement.Start else Arrangement.End
                        ) {
                            if (isCompleted) {
                                // Показываем индикатор для снятия отметки
                                Text(
                                    text = "Сдвиньте вправо, чтобы снять отметку о выполнении",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                // Показываем индикатор для установки отметки
                                Text(
                                    text = "Сдвиньте влево, чтобы отметить как выполненное",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Индикаторы действия для свайпа
        if (onToggleCompletion != null && offsetX != 0f &&
            !action.isSkipped && hasMultipleFactActions) {

            // Индикатор слева (для отметки выполнения)
            if (!isCompleted && offsetX < -75) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                        .offset { IntOffset(16.dp.toPx().toInt(), 0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Отметить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Индикатор справа (для снятия отметки)
            if (isCompleted && offsetX > 75) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .offset { IntOffset(-16.dp.toPx().toInt(), 0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Снять отметку",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}