package com.synngate.synnframe.domain.model.wizard

/**
 * Контекст для шага визарда действий
 */
data class ActionContext(
    val taskId: String,
    val actionId: String,
    val stepId: String,
    val results: Map<String, Any>,
    val onUpdate: (Map<String, Any>) -> Unit, // Обновление результатов шага
    val onComplete: (Any?) -> Unit,           // Завершение шага с результатом
    val onBack: () -> Unit,                   // Возврат к предыдущему шагу
    val onSkip: (Any?) -> Unit,               // Пропуск шага с опциональным результатом
    val onCancel: () -> Unit                  // Отмена визарда
)