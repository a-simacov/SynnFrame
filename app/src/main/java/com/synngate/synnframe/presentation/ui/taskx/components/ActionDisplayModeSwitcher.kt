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

/**
 * Статические данные о возможных режимах отображения.
 */
private val ALL_DISPLAY_MODES = mapOf(
    ActionDisplayMode.CURRENT to "Текущие",
    ActionDisplayMode.PARTIALLY_COMPLETED to "Частично",
    ActionDisplayMode.COMPLETED to "Выполненные",
    ActionDisplayMode.ALL to "Все",
    ActionDisplayMode.FINALS to "Финальные"
)

/**
 * Компонент для переключения режимов отображения действий
 */
@Composable
fun ActionDisplayModeSwitcher(
    currentMode: ActionDisplayMode,
    onModeChange: (ActionDisplayMode) -> Unit,
    hasFinalActions: Boolean,
    hasPartiallyCompletedActions: Boolean = false, // Добавляем параметр для частично выполненных действий
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Используем remember с ключами для кэширования списка
    val displayModes = remember(hasFinalActions, hasPartiallyCompletedActions) {
        buildList {
            add(ActionDisplayMode.CURRENT)

            // Добавляем режим "Частично" только если есть частично выполненные действия
            if (hasPartiallyCompletedActions) {
                add(ActionDisplayMode.PARTIALLY_COMPLETED)
            }

            add(ActionDisplayMode.COMPLETED)
            add(ActionDisplayMode.ALL)

            // Добавляем режим "Финальные" только если есть финальные действия
            if (hasFinalActions) {
                add(ActionDisplayMode.FINALS)
            }
        }
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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