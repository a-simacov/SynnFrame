package com.synngate.synnframe.presentation.ui.taskx.model

/**
 * Результат завершения задания для отображения в диалоге
 */
data class TaskCompletionResult(
    val message: String,
    val isSuccess: Boolean
)