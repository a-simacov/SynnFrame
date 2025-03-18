// Файл: com.synngate.synnframe.presentation.ui.logs.LogListState.kt

package com.synngate.synnframe.presentation.ui.logs.model

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import java.time.LocalDateTime

/**
 * Состояние экрана списка логов
 */
data class LogListState(
    // Список логов для отображения
    val logs: List<Log> = emptyList(),

    // Статус загрузки
    val isLoading: Boolean = false,

    // Сообщение об ошибке
    val error: String? = null,

    // Фильтры
    val messageFilter: String = "",
    val selectedTypes: Set<LogType> = emptySet(),
    val dateFromFilter: LocalDateTime? = null,
    val dateToFilter: LocalDateTime? = null,

    // Флаг видимости панели фильтров
    val isFilterPanelVisible: Boolean = false
) {
    /**
     * Проверка, применены ли какие-либо фильтры
     */
    val hasActiveFilters: Boolean
        get() = messageFilter.isNotEmpty() || selectedTypes.isNotEmpty() ||
                (dateFromFilter != null && dateToFilter != null)
}