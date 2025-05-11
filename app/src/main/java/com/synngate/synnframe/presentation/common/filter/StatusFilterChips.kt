package com.synngate.synnframe.presentation.common.filter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synngate.synnframe.R


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
        if (allItem != null) {
            StatusFilterChip(
                text = allItemText,
                selected = selectedItems.isEmpty(),
                onClick = {
                    onSelectionChanged(emptySet())
                }
            )
        }

        items.forEach { item ->
            if (item != allItem) {
                StatusFilterChip(
                    text = itemToString(item),
                    selected = selectedItems.contains(item),
                    onClick = {
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