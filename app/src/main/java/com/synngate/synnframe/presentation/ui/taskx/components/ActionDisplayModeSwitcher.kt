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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.model.ActionDisplayMode

private val ALL_DISPLAY_MODES = mapOf(
    ActionDisplayMode.CURRENT to "Текущие",
    ActionDisplayMode.COMPLETED to "Выполненные",
    ActionDisplayMode.ALL to "Все",
    ActionDisplayMode.FINALS to "Финальные",
    ActionDisplayMode.INITIALS to "Начальные"
)

@Composable
fun ActionDisplayModeSwitcher(
    currentMode: ActionDisplayMode,
    onModeChange: (ActionDisplayMode) -> Unit,
    hasFinalActions: Boolean,
    hasInitialActions: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val displayModes = remember(hasFinalActions, hasInitialActions) {
        buildList {
            add(ActionDisplayMode.CURRENT)
            add(ActionDisplayMode.COMPLETED)
            add(ActionDisplayMode.ALL)

            if (hasInitialActions) {
                add(ActionDisplayMode.INITIALS)
            }

            if (hasFinalActions) {
                add(ActionDisplayMode.FINALS)
            }
        }
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        displayModes.forEach { mode ->
            val label = ALL_DISPLAY_MODES[mode] ?: "Неизвестно"
            val isSelected = currentMode == mode

            FilterChip(
                label = { Text(label) },
                selected = isSelected,
                onClick = { onModeChange(mode) },
                leadingIcon = if (isSelected) {
                    // Используем remember для предотвращения пересоздания лямбды
                    remember {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    }
                } else null
            )
        }
    }
}