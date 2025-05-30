package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.buffer.BufferDisplayItem

/**
 * Компонент для отображения элементов буфера задания
 */
@Composable
fun BufferItemChipList(
    items: List<BufferDisplayItem>,
    onRemove: (FactActionField) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Буфер задания",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Буфер задания:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )

                if (items.size > 1) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Очистить буфер")
                    }
                }
            }

            items.forEach { item ->
                FilterChip(
                    modifier = Modifier.fillMaxWidth(),
                    selected = true,
                    onClick = { /* Чип уже выбран, ничего не делаем */ },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.displayName}: ${item.value}",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "из ${item.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onRemove(item.field) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить из буфера"
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}