package com.synngate.synnframe.presentation.ui.wizard.action.base

data class StepViewState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val additionalData: Map<String, Any> = emptyMap(),
    val autoFilled: Boolean = false
) {

    fun isAutoFilled(): Boolean {
        return autoFilled || additionalData["autoFilled"] == true
    }

    fun getMarkedForSavingObject(type: String): Any? {
        return additionalData["markedForSaving_$type"]
    }
}