package com.synngate.synnframe.domain.model.wizard

data class ActionContext(
    val taskId: String,
    val actionId: String,
    val stepId: String,
    val results: Map<String, Any>,
    val hasStepResult: Boolean = false,
    val onUpdate: (Map<String, Any>) -> Unit,
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit,
    val onForward: () -> Unit,
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit,
    val lastScannedBarcode: String? = null,
    val validationError: String? = null,
    // Добавляем флаг для отслеживания глобального состояния обработки шага
    val isProcessingStep: Boolean = false,
    // Добавляем флаг для определения, является ли шаг первым
    val isFirstStep: Boolean = false
) {

    fun getCurrentStepResult(): Any? {
        return if (hasStepResult) results[stepId] else null
    }
}