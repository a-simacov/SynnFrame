package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import java.time.LocalDateTime

@Composable
fun PlannedActionsView(
    plannedActions: List<PlannedAction>,
    onActionClick: (PlannedAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Запланированные действия",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

        if (plannedActions.isEmpty()) {
            EmptyScreenContent(message = "Нет запланированных действий")
        } else {
            LazyColumn {
                items(plannedActions.sortedBy { it.order }) { action ->
                    PlannedActionItem(
                        action = action,
                        onClick = { onActionClick(action) }
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
        Text(
            text = "Выполненные действия",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        action.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
        action.isSkipped -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = !action.isCompleted && !action.isSkipped, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.actionTemplate.name,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    Text(
                        text = "Тип: ${action.wmsAction.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Иконка статуса
                when {
                    action.isCompleted -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Выполнено",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    action.isSkipped -> {
                        Icon(
                            imageVector = Icons.Default.NoEncryption,
                            contentDescription = "Пропущено",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.PendingActions,
                            contentDescription = "Ожидает выполнения",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Отображение товара, если есть
            action.storageProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Отображение паллеты хранения, если есть
            action.storagePallet?.let { pallet ->
                Text(
                    text = "Паллета хранения: ${pallet.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Отображение ячейки размещения, если есть
            action.placementBin?.let { bin ->
                Text(
                    text = "Ячейка размещения: ${bin.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Отображение паллеты размещения, если есть
            action.placementPallet?.let { pallet ->
                Text(
                    text = "Паллета размещения: ${pallet.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
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
            Text(
                text = "Действие WMS: ${action.wmsAction.name}",
                style = MaterialTheme.typography.titleSmall
            )

            action.storageProduct?.let { ShowStorageProduct(it) }
            action.storagePallet?.let { ShowPallet("Паллета хранения", it) }
            action.placementBin?.let { ShowBin(it) }
            action.placementPallet?.let { ShowPallet("Паллета размещения", it) }

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

    Text(
        text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
        style = MaterialTheme.typography.bodySmall
    )
}