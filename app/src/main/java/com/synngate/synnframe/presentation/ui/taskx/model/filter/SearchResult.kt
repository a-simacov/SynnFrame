package com.synngate.synnframe.presentation.ui.taskx.model.filter

import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

sealed class SearchResult {

    data class Success(
        val field: FactActionField,
        val value: Any,
        val actionIds: List<String>
    ) : SearchResult()

    data class NotFound(val message: String) : SearchResult()

    data class Error(val message: String) : SearchResult()
}