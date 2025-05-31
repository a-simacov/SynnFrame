package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.model.ActionFilter

@Composable
fun ActionFilterChips(
    currentFilter: ActionFilter,
    onFilterChange: (ActionFilter) -> Unit,
    hasInitialActions: Boolean,
    hasFinalActions: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Определяем доступные фильтры
    val availableFilters = buildList {
        add(ActionFilter.ALL)
        add(ActionFilter.PENDING)
        add(ActionFilter.COMPLETED)

        if (hasInitialActions) {
            add(ActionFilter.INITIAL)
        }

        add(ActionFilter.REGULAR)

        if (hasFinalActions) {
            add(ActionFilter.FINAL)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        availableFilters.forEach { filter ->
            FilterChip(
                modifier = Modifier.height(32.dp),
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.displayName) },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}