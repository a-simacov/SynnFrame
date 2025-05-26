package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Карточка для отображения планового действия
 */
@Composable
fun PlannedActionCard(
    action: PlannedAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isCompleted = action.isCompleted || action.manuallyCompleted

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isCompleted) { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 1.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Иконка статуса
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Основное содержимое
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Название действия
                Text(
                    text = action.actionTemplate?.name ?: "Действие #${action.order}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Информация о действии
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Товар
                    if (action.storageProductClassifier != null || action.storageProduct != null) {
                        val productName = action.storageProductClassifier?.name
                            ?: action.storageProduct?.product?.name
                            ?: "Неизвестный товар"

                        InfoRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = productName
                        )
                    }

                    // Места хранения/размещения
                    val locations = buildList {
                        action.storageBin?.let { add("Из: ${it.code}") }
                        action.storagePallet?.let { add("Паллета: ${it.code}") }
                        action.placementBin?.let { add("В: ${it.code}") }
                        action.placementPallet?.let { add("На паллету: ${it.code}") }
                    }

                    if (locations.isNotEmpty()) {
                        InfoRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = locations.joinToString(", ")
                        )
                    }

                    // Количество
                    if (action.quantity > 0) {
                        Text(
                            text = "Количество: ${action.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Тип действия (начальное/финальное)
                when {
                    action.isInitialAction() -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        ActionTypeChip(
                            text = "Начальное",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    action.isFinalAction() -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        ActionTypeChip(
                            text = "Завершающее",
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Номер действия
            Text(
                text = "#${action.order}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionTypeChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}