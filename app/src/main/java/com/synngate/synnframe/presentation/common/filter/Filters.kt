package com.synngate.synnframe.presentation.common.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.R

@Composable
fun FilterPanel(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Заголовок панели фильтров
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(id = R.string.filter),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.filter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { onVisibilityChange(!isVisible) }
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Close else Icons.Default.FilterList,
                    contentDescription = if (isVisible) stringResource(id = R.string.close)
                    else stringResource(id = R.string.filter)
                )
            }
        }

        HorizontalDivider()

        // Содержимое панели фильтров
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun <T> StatusFilterChips(
    items: List<T>,
    selectedItems: Set<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    itemToString: (T) -> String,
    modifier: Modifier = Modifier,
    allItem: T? = null,
    allItemText: String = stringResource(id = R.string.task_status_all)
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Если есть специальный элемент "Все", отображаем его отдельно
        if (allItem != null) {
            StatusFilterChip(
                text = allItemText,
                selected = selectedItems.isEmpty(),
                onClick = {
                    // При выборе "Все" очищаем все выбранные фильтры
                    onSelectionChanged(emptySet())
                }
            )
        }

        // Отображаем остальные элементы
        items.forEach { item ->
            if (item != allItem) {
                StatusFilterChip(
                    text = itemToString(item),
                    selected = selectedItems.contains(item),
                    onClick = {
                        // Обновляем выбранные фильтры
                        val newSelection = selectedItems.toMutableSet()
                        if (selectedItems.contains(item)) {
                            newSelection.remove(item)
                        } else {
                            newSelection.add(item)
                        }
                        onSelectionChanged(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 16.sp) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else null
    )
}