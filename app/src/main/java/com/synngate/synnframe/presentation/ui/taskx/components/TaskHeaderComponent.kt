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

@Composable
fun TaskHeaderComponent(
    task: TaskX,
    showSearchBar: Boolean,
    showBufferItems: Boolean,
    showFilters: Boolean,
    hasBufferItems: Boolean,
    hasFilters: Boolean,
    onToggleSearch: () -> Unit,
    onToggleBufferDisplay: () -> Unit,
    onToggleFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskProgressIndicator(
            task = task,
            modifier = Modifier.weight(1f)
        )

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

        if (hasFilters) {
            IconButton(
                onClick = onToggleFilters,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = if (showFilters) "Скрыть фильтры" else "Показать фильтры",
                    tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

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
    }
}