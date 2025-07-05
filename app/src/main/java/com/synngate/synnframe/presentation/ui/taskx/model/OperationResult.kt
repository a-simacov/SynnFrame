package com.synngate.synnframe.presentation.ui.taskx.model

sealed class OperationResult {
    data class Success(val message: String? = null) : OperationResult()
    data class Error(val message: String) : OperationResult()
    data class UserMessage(val message: String, val isSuccess: Boolean) : OperationResult()
}