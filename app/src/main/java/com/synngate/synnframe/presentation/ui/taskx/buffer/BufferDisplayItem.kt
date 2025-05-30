package com.synngate.synnframe.presentation.ui.taskx.model.buffer

import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

/**
 * Модель для отображения элемента буфера в UI
 */
data class BufferDisplayItem(
    val field: FactActionField,
    val displayName: String,
    val value: String,
    val data: Any,
    val source: String // откуда взято значение: "wizard", "filter" и т.д.
)