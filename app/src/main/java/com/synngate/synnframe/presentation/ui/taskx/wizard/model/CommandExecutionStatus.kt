package com.synngate.synnframe.presentation.ui.taskx.wizard.model

/**
 * Модель для хранения статуса выполнения команды
 */
data class CommandExecutionStatus(
    val commandId: String,
    val stepId: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String? = null
)