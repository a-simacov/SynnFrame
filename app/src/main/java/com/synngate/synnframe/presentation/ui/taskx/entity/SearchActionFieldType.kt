package com.synngate.synnframe.presentation.ui.taskx.entity

import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import kotlinx.serialization.Serializable

@Serializable
data class SearchActionFieldType(
    val actionField: FactActionField,
    val isRemoteSearch: Boolean,
    val endpoint: String = "",
    val saveToTaskBuffer: Boolean = false
)