package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.SavableObject
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.presentation.ui.taskx.model.ActionDisplayMode
import com.synngate.synnframe.presentation.ui.taskx.model.TaskXDetailView
import com.synngate.synnframe.presentation.ui.wizard.action.components.getObjectVisualData
import java.time.LocalDateTime

@Composable
fun CompactTaskInfoCard(
    task: TaskX,
    taskTypeName: String,
    formatDate: (LocalDateTime?) -> String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onShowPlannedActions: () -> Unit,
    onShowFactActions: () -> Unit,
    activeView: TaskXDetailView
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$taskTypeName (${task.barcode})",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.padding(4.dp)
                )
            }
            CompactTaskProgressIndicator(task = task)

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Штрихкод: ${task.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    task.startedAt?.let {
                        Text(
                            text = "Начато: ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    task.lastModifiedAt?.let {
                        Text(
                            text = "Изменено: ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SegmentedButton(
                            selected = activeView == TaskXDetailView.PLANNED_ACTIONS,
                            onClick = onShowPlannedActions,
                            label = "План",
                            modifier = Modifier.weight(1f)
                        )

                        SegmentedButton(
                            selected = activeView == TaskXDetailView.FACT_ACTIONS,
                            onClick = onShowFactActions,
                            label = "Факт",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .padding(horizontal = 2.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun CompactTaskProgressIndicator(
    task: TaskX,
    modifier: Modifier = Modifier
) {
    val regularActions = task.plannedActions.filter { !it.isFinalAction && !it.isInitialAction }
    val finalActions = task.plannedActions.filter { it.isFinalAction }
    val initialActions = task.plannedActions.filter { it.isInitialAction }

    val completedRegularActions = regularActions.count { it.isCompleted }
    val completedFinalActions = finalActions.count { it.isCompleted }
    val completedInitialActions = initialActions.count { it.isCompleted }

    val totalProgress = if (task.plannedActions.isNotEmpty()) {
        task.plannedActions.count { it.isCompleted }.toFloat() / task.plannedActions.size
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$completedRegularActions/${regularActions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (initialActions.isNotEmpty()) {
                    Text(
                        text = "🚀 $completedInitialActions/${initialActions.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (finalActions.isNotEmpty()) {
                    Text(
                        text = "⚡ $completedFinalActions/${finalActions.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CompactActionDisplayModeSwitcher(
    currentMode: ActionDisplayMode,
    onModeChange: (ActionDisplayMode) -> Unit,
    hasFinalActions: Boolean,
    hasInitialActions: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val displayModes = remember(hasFinalActions, hasInitialActions) {
        val modes = mutableListOf(
            Pair(ActionDisplayMode.CURRENT, Icons.Default.PlayArrow),
            Pair(ActionDisplayMode.COMPLETED, Icons.Default.CheckCircle),
            Pair(ActionDisplayMode.ALL, Icons.Default.List)
        )

        if (hasInitialActions) {
            modes.add(Pair(ActionDisplayMode.INITIALS, Icons.Default.Star))
        }

        if (hasFinalActions) {
            modes.add(Pair(ActionDisplayMode.FINALS, Icons.Default.Flag))
        }

        modes
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        displayModes.forEach { (mode, icon) ->
            val isSelected = currentMode == mode

            FilterChip(
                selected = isSelected,
                onClick = { onModeChange(mode) },
                label = {
                    Text(getModeShortLabel(mode))
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

private fun getModeShortLabel(mode: ActionDisplayMode): String {
    return when (mode) {
        ActionDisplayMode.CURRENT -> "Текущие"
        ActionDisplayMode.COMPLETED -> "Выполнено"
        ActionDisplayMode.ALL -> "Все"
        ActionDisplayMode.FINALS -> "Итоговые"
        ActionDisplayMode.INITIALS -> "Начальные"
    }
}

@Composable
fun CompactActionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onScannerClick: () -> Unit,
    isSearching: Boolean,
    error: String?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Column(modifier = modifier) {
        AnimatedVisibility(visible = visible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small
                    ),
            ) {
                // Поисковое поле с уменьшенными отступами
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Поиск", style = MaterialTheme.typography.bodySmall) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = onSearch) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Поиск",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Кнопка сканера штрих-кода
                IconButton(
                    onClick = onScannerClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Сканировать",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Отображение ошибки (если есть)
        AnimatedVisibility(visible = error != null) {
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CompactSavableObjectsPanel(
    savableObjects: List<SavableObject>,
    onRemoveObject: (String) -> Unit,
    onFilterByObjects: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible || savableObjects.isEmpty()) return

    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = visible && savableObjects.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Прокручиваемый список объектов в виде чипов
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    savableObjects.forEach { savableObject ->
                        CompactSavableObjectChip(
                            savableObject = savableObject,
                            onRemove = onRemoveObject
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                // Кнопка фильтрации
                FilledIconButton(
                    onClick = onFilterByObjects,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Фильтровать",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSavableObjectChip(
    savableObject: SavableObject,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconVector, description, label, chipColor) = getObjectVisualData(savableObject)

    AssistChip(
        onClick = { /* Нет действия при клике */ },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = iconVector,
                contentDescription = description,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { onRemove(savableObject.id) },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(12.dp)
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = chipColor.copy(alpha = 0.15f),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null,
        modifier = modifier
    )
}

@Composable
fun CompactNextActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.alpha(0.5f),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Выполнить следующее действие"
        )
    }
}