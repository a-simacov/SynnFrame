package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.TaskStatus

/**
 * Группа фильтров-чипов для статусов заданий
 */
@Composable
fun TaskStatusFilterChips(
    selectedStatuses: Set<TaskStatus>,
    onStatusSelected: (TaskStatus) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Чип "К выполнению"
        StatusFilterChip(
            text = stringResource(R.string.task_status_to_do),
            selected = selectedStatuses.contains(TaskStatus.TO_DO),
            onClick = { onStatusSelected(TaskStatus.TO_DO) }
        )

        // Чип "Выполняется"
        StatusFilterChip(
            text = stringResource(R.string.task_status_in_progress),
            selected = selectedStatuses.contains(TaskStatus.IN_PROGRESS),
            onClick = { onStatusSelected(TaskStatus.IN_PROGRESS) }
        )

        // Чип "Выполнено"
        StatusFilterChip(
            text = stringResource(R.string.task_status_completed),
            selected = selectedStatuses.contains(TaskStatus.COMPLETED),
            onClick = { onStatusSelected(TaskStatus.COMPLETED) }
        )

        // Чип "Все"
        StatusFilterChip(
            text = stringResource(R.string.all),
            selected = selectedStatuses.isEmpty(),
            onClick = { onClearFilters() }
        )
    }
}

/**
 * Отдельный фильтр-чип для статуса
 */
@Composable
fun StatusFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
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