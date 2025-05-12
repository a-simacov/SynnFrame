package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Announcement
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import java.time.LocalDateTime

@Composable
fun PlannedActionsView(
    plannedActions: List<PlannedAction>,
    factActions: List<FactAction>,
    onActionClick: (PlannedAction) -> Unit,
    onToggleCompletion: ((PlannedAction, Boolean) -> Unit)? = null,
    nextActionId: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (plannedActions.isEmpty()) {
            EmptyScreenContent(message = "Нет запланированных действий")
        } else {
            LazyColumn {
                items(plannedActions.sortedBy { it.order }) { action ->
                    // Используем новый компонент с поддержкой свайпа
                    PlannedActionItem(
                        action = action,
                        factActions = factActions,
                        onClick = { onActionClick(action) },
                        onToggleCompletion = onToggleCompletion?.let { toggle ->
                            { completed -> toggle(action, completed) }
                        },
                        isNextAction = action.id == nextActionId
                    )
                }
            }
        }
    }
}

@Composable
fun FactActionsView(
    factActions: List<FactAction>,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (factActions.isEmpty()) {
            EmptyScreenContent(message = "Нет выполненных действий")
        } else {
            LazyColumn {
                items(factActions.sortedByDescending { it.completedAt }) { action ->
                    FactActionItem(
                        action = action,
                        formatDate = formatDate
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedActionItem(
    action: PlannedAction,
    factActions: List<FactAction>,
    onClick: () -> Unit,
    onToggleCompletion: ((Boolean) -> Unit)? = null,
    isNextAction: Boolean = false,
    modifier: Modifier = Modifier
) {
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
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    // Определяем иконку и описание статуса
    val (statusIcon, statusDescription) = when {
        isCompleted && isManuallyCompleted -> Pair(
            Icons.Default.CheckCircle,
            "Завершено вручную${action.manuallyCompleted}"
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = action.isClickable(), onClick = onClick),
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
                            style = MaterialTheme.typography.bodySmall,
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

                // Добавляем выпадающее меню для управления статусом выполнения
                if (onToggleCompletion != null && hasMultipleFactActions && !action.isQuantityFulfilled(factActions)) {
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Действия",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isCompleted)
                                            "Снять отметку о выполнении"
                                        else
                                            "Отметить как выполненное"
                                    )
                                },
                                onClick = {
                                    onToggleCompletion(!isCompleted)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCompleted)
                                            Icons.Default.Close
                                        else
                                            Icons.Default.CheckCircle,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Дополнительная информация о товаре, если есть
            action.storageProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Показываем количество для действий с учетом количества
                if (hasMultipleFactActions && plannedQuantity > 0) {
                    Text(
                        text = "Количество: $completedQuantity / $plannedQuantity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Отображение паллеты хранения, если есть
            action.storagePallet?.let { pallet ->
                Text(
                    text = "Паллета хранения: ${pallet.code}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

//            // Информация о типе действия
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.padding(top = 4.dp)
//            ) {
//                WmsActionIconWithTooltip(
//                    wmsAction = action.wmsAction,
//                    iconSize = 18
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//                Text(
//                    text = getWmsActionDescription(action.wmsAction),
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }

            // Отображение ячейки размещения, если есть
            action.placementBin?.let { bin ->
                Text(
                    text = "Ячейка размещения: ${bin.code}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Отображение паллеты размещения, если есть
            action.placementPallet?.let { pallet ->
                Text(
                    text = "Паллета размещения: ${pallet.code}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Индикатор прогресса для действий с учетом количества
            if (hasMultipleFactActions && !action.isSkipped) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Прогресс-бар
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f),
                        color = progressColor
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Количество фактических действий
                    val relatedFacts = factActions.filter { it.plannedActionId == action.id }
                    if (relatedFacts.isNotEmpty()) {
                        Text(
                            text = "(${relatedFacts.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FactActionItem(
    action: FactAction,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с иконкой типа действия и подсказкой
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                WmsActionIconWithTooltip(
                    wmsAction = action.wmsAction,
                    iconSize = 24
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = getWmsActionDescription(action.wmsAction),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(4.dp))

            action.storageProduct?.let { ShowStorageProduct(it) }
            action.storagePallet?.let { ShowPallet("Паллета хранения", it) }
            action.placementBin?.let { ShowBin(it) }
            action.placementPallet?.let { ShowPallet("Паллета размещения", it) }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Начато: ${formatDate(action.startedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Завершено: ${formatDate(action.completedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ShowStorageProduct(product: TaskProduct) {
    Text(
        text = "Товар: ${product.product.name}",
        style = MaterialTheme.typography.bodyMedium
    )

    Text(
        text = "Артикул: ${product.product.articleNumber}",
        style = MaterialTheme.typography.bodySmall
    )

    Text(
        text = "Количество: ${product.quantity}",
        style = MaterialTheme.typography.bodyMedium
    )

    if (product.hasExpirationDate()) {
        Text(
            text = "Срок годности: ${product.expirationDate}",
            style = MaterialTheme.typography.bodySmall
        )
    }

    Text(
        text = "Статус: ${product.status}",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ShowPallet(title: String, pallet: Pallet) {
    Text(
        text = "$title: ${pallet.code}",
        style = MaterialTheme.typography.bodyMedium
    )

    Text(
        text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ShowBin(bin: BinX) {
    Text(
        text = "Ячейка: ${bin.code}",
        style = MaterialTheme.typography.bodyMedium
    )

    Text(
        text = "Зона: ${bin.zone}",
        style = MaterialTheme.typography.bodySmall
    )
}