package com.synngate.synnframe.presentation.ui.taskx.model.filter

import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

data class FilterItem(
    val field: FactActionField,
    val displayName: String,
    val value: String,
    val data: Any,
    val timestamp: Long = System.currentTimeMillis()
)