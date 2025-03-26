package com.synngate.synnframe.presentation.ui.logs.model

/*
 * Обратите внимание: для применения этого файла:
 * 1. Обновите существующий файл LogListState.kt с этим содержимым
 * 2. Обновите существующий файл LogEvents.kt, добавив события ShowDateFilterDialog и HideDateFilterDialog
 */

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.presentation.common.dialog.DateFilterPreset
import java.time.LocalDateTime

data class LogListState(
    val logs: List<Log> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val messageFilter: String = "",
    val selectedTypes: Set<LogType> = emptySet(),
    val dateFromFilter: LocalDateTime? = null,
    val dateToFilter: LocalDateTime? = null,
    val isDateFilterDialogVisible: Boolean = false,
    val activeDateFilterPreset: DateFilterPreset? = null
) {
    val hasActiveFilters: Boolean
        get() = messageFilter.isNotEmpty() || selectedTypes.isNotEmpty() ||
                (dateFromFilter != null && dateToFilter != null)

    val hasActiveDateFilter: Boolean
        get() = dateFromFilter != null && dateToFilter != null
}