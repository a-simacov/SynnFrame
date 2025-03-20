// Файл: com.synngate.synnframe.presentation.ui.logs.LogListState.kt

package com.synngate.synnframe.presentation.ui.logs.model

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import java.time.LocalDateTime

data class LogListState(

    val logs: List<Log> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null,

    val messageFilter: String = "",
    val selectedTypes: Set<LogType> = emptySet(),
    val dateFromFilter: LocalDateTime? = null,
    val dateToFilter: LocalDateTime? = null,

    val isFilterPanelVisible: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = messageFilter.isNotEmpty() || selectedTypes.isNotEmpty() ||
                (dateFromFilter != null && dateToFilter != null)
}