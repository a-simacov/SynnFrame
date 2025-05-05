package com.synngate.synnframe.util

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.ProgressType

/**
 * Утилитарный класс для работы с прогрессом действий.
 * Содержит вспомогательные функции для анализа прогресса выполнения действий.
 */
object ActionProgressUtil {

    /**
     * Результаты анализа прогресса действия
     */
    data class ActionProgressInfo(
        val actionId: String,
        val name: String,
        val isCompleted: Boolean,
        val isSkipped: Boolean,
        val manuallyCompleted: Boolean,
        val plannedQuantity: Float,
        val completedQuantity: Float,
        val progressPercent: Float,
        val factActionsCount: Int
    )

    /**
     * Анализирует прогресс выполнения всех действий в задании
     * @param task Задание, для которого выполняется анализ
     * @return Список информации о прогрессе действий
     */
    fun analyzeTaskProgress(task: TaskX): List<ActionProgressInfo> {
        return task.plannedActions.map { action ->
            analyzeActionProgress(action, task.factActions)
        }
    }

    /**
     * Анализирует прогресс выполнения конкретного действия
     * @param action Запланированное действие
     * @param factActions Список фактических действий
     * @return Информация о прогрессе действия
     */
    fun analyzeActionProgress(
        action: PlannedAction,
        factActions: List<FactAction>
    ): ActionProgressInfo {
        // Фильтруем фактические действия, связанные с данным плановым
        val relatedFacts = factActions.filter { it.plannedActionId == action.id }

        // Вычисляем выполненное количество (для действий с учетом количества)
        val completedQuantity = relatedFacts.sumOf {
            it.storageProduct?.quantity?.toDouble() ?: 0.0
        }.toFloat()

        // Получаем плановое количество
        val plannedQuantity = action.storageProduct?.quantity ?: 0f

        // Вычисляем процент выполнения
        val progressPercent = if (action.progressType == ProgressType.QUANTITY && plannedQuantity > 0f) {
            (completedQuantity / plannedQuantity).coerceIn(0f, 1f) * 100f
        } else if (action.isCompleted || action.manuallyCompleted) {
            100f
        } else {
            0f
        }

        return ActionProgressInfo(
            actionId = action.id,
            name = action.actionTemplate.name,
            isCompleted = action.isCompleted,
            isSkipped = action.isSkipped,
            manuallyCompleted = action.manuallyCompleted,
            plannedQuantity = plannedQuantity,
            completedQuantity = completedQuantity,
            progressPercent = progressPercent,
            factActionsCount = relatedFacts.size
        )
    }

    /**
     * Генерирует отчет о прогрессе выполнения задания
     * @param task Задание, для которого генерируется отчет
     * @return Текстовый отчет о прогрессе выполнения
     */
    fun generateProgressReport(task: TaskX): String {
        val progressInfo = analyzeTaskProgress(task)
        val overallProgress = if (progressInfo.isNotEmpty()) {
            progressInfo.sumOf { it.progressPercent.toDouble() } / progressInfo.size
        } else {
            0.0
        }

        val sb = StringBuilder()
        sb.appendLine("=== Отчет о прогрессе выполнения задания ===")
        sb.appendLine("Задание: ${task.name} (${task.id})")
        sb.appendLine("Статус: ${task.status}")
        sb.appendLine("Общий прогресс: ${String.format("%.1f", overallProgress)}%")
        sb.appendLine("Количество запланированных действий: ${task.plannedActions.size}")
        sb.appendLine("Количество фактических действий: ${task.factActions.size}")
        sb.appendLine("\nДействия:")

        progressInfo.forEach { info ->
            sb.appendLine("- ${info.name} (${info.actionId})")
            sb.appendLine("  Статус: ${getActionStatusDescription(info)}")

            if (info.plannedQuantity > 0f) {
                sb.appendLine("  Количество: ${info.completedQuantity} / ${info.plannedQuantity}")
                sb.appendLine("  Прогресс: ${String.format("%.1f", info.progressPercent)}%")
            }

            sb.appendLine("  Фактических действий: ${info.factActionsCount}")
        }

        return sb.toString()
    }

    /**
     * Возвращает текстовое описание статуса действия
     */
    private fun getActionStatusDescription(info: ActionProgressInfo): String {
        return when {
            info.isSkipped -> "Пропущено"
            info.isCompleted && info.manuallyCompleted -> "Выполнено вручную"
            info.isCompleted -> "Выполнено"
            info.manuallyCompleted -> "Отмечено как выполненное вручную"
            info.progressPercent > 0f -> "Частично выполнено (${String.format("%.1f", info.progressPercent)}%)"
            else -> "Не выполнено"
        }
    }
}