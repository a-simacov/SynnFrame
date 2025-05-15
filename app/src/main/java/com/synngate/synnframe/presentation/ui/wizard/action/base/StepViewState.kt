package com.synngate.synnframe.presentation.ui.wizard.action.base

data class StepViewState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
)