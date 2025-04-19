package com.synngate.synnframe.domain.model.wizard

/**
 * Контекст для шага визарда действий
 */
data class ActionContext(
    val taskId: String,
    val actionId: String,
    val stepId: String,
    val results: Map<String, Any>,
    val hasStepResult: Boolean = false,       // Флаг, указывающий, что для текущего шага уже есть результат
    val onUpdate: (Map<String, Any>) -> Unit, // Обновление результатов шага
    val onComplete: (Any?) -> Unit,           // Завершение шага с результатом
    val onBack: () -> Unit,                   // Возврат к предыдущему шагу
    val onForward: () -> Unit,                // Переход к следующему шагу без изменения результата
    val onSkip: (Any?) -> Unit,               // Пропуск шага с опциональным результатом
    val onCancel: () -> Unit                  // Отмена визарда
) {
    /**
     * Получает результат для текущего шага, если он есть
     */
    fun getCurrentStepResult(): Any? {
        return if (hasStepResult) results[stepId] else null
    }
}