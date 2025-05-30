package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskX

/**
 * Компонент заголовка задания, объединяющий индикатор прогресса и кнопки управления
 */
@Composable
fun TaskHeaderComponent(
    task: TaskX,
    showSearchBar: Boolean,
    showBufferItems: Boolean,
    hasBufferItems: Boolean,
    hasFilters: Boolean,
    onToggleSearch: () -> Unit,
    onToggleBufferDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Индикатор прогресса
        TaskProgressIndicator(
            task = task,
            modifier = Modifier.weight(1f)
        )

        // Кнопка поиска
        if (task.taskType?.isActionSearchEnabled() == true) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (showSearchBar) Icons.Default.SearchOff else Icons.Default.Search,
                    contentDescription = if (showSearchBar) "Скрыть поиск" else "Показать поиск",
                    tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Кнопка буфера
        if (hasBufferItems) {
            IconButton(
                onClick = onToggleBufferDisplay,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = if (showBufferItems) "Скрыть буфер" else "Показать буфер",
                    tint = if (showBufferItems) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Индикатор активных фильтров
        if (hasFilters) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Активные фильтры",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}