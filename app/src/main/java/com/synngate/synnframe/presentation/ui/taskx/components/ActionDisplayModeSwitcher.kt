package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.model.ActionDisplayMode

@Composable
fun ActionDisplayModeSwitcher(
    currentMode: ActionDisplayMode,
    onModeChange: (ActionDisplayMode) -> Unit,
    hasFinalActions: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            label = { Text( "Текущие" ) },
            selected = currentMode == ActionDisplayMode.CURRENT,
            onClick = { onModeChange(ActionDisplayMode.CURRENT) },
            leadingIcon = if (currentMode == ActionDisplayMode.CURRENT) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )

        FilterChip(
            label = { Text( "Выполненные") },
            selected = currentMode == ActionDisplayMode.COMPLETED,
            onClick = { onModeChange(ActionDisplayMode.COMPLETED) },
            leadingIcon = if (currentMode == ActionDisplayMode.COMPLETED) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )

        FilterChip(
            label = { Text( "Все") },
            selected = currentMode == ActionDisplayMode.ALL,
            onClick = { onModeChange(ActionDisplayMode.ALL) },
            leadingIcon = if (currentMode == ActionDisplayMode.ALL) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )

        if (hasFinalActions) {
            FilterChip(
                label = { Text( "Финальные") },
                selected = currentMode == ActionDisplayMode.FINALS,
                onClick = { onModeChange(ActionDisplayMode.FINALS) },
                leadingIcon = if (currentMode == ActionDisplayMode.FINALS) {
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
    }
}