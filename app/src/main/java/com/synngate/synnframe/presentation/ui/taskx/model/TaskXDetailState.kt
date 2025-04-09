package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import java.time.LocalDateTime

enum class TaskXDetailView {
    COMPARED_LINES,  // Сравнение строк плана и факта (по умолчанию)
    PLAN_LINES,      // Только строки плана
    FACT_LINES       // Только строки факта
}

data class TaskXDetailState(
    val task: TaskX? = null,
    val taskType: TaskTypeX? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val activeView: TaskXDetailView = TaskXDetailView.COMPARED_LINES,
    val showVerificationDialog: Boolean = false,
    val currentUserId: String? = null
)

sealed class TaskXDetailEvent {
    data class ShowSnackbar(val message: String) : TaskXDetailEvent()
    data object ShowFactLineWizard : TaskXDetailEvent()
    data object HideFactLineWizard : TaskXDetailEvent()
}

data class FactLineWizardUiState(
    val taskId: String,
    val groups: List<FactLineActionGroup>,
    val currentGroupIndex: Int = 0,
    val currentActionIndex: Int = 0,
    // Промежуточные результаты
    val intermediateResults: Map<String, Any?> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val startedAt: LocalDateTime = LocalDateTime.now()
) {
    val currentGroup: FactLineActionGroup?
        get() = if (currentGroupIndex < groups.size) groups[currentGroupIndex] else null

    val currentAction
        get() = currentGroup?.actions?.getOrNull(currentActionIndex)

    val isLastAction: Boolean
        get() {
            val group = currentGroup ?: return true
            return currentActionIndex >= group.actions.size - 1
        }

    val isLastGroup: Boolean
        get() = currentGroupIndex >= groups.size - 1

    val progress: Float
        get() {
            if (groups.isEmpty()) return 0f

            var completedSteps = 0
            var totalSteps = 0

            groups.forEachIndexed { groupIndex, group ->
                val actionsCount = group.actions.size
                totalSteps += actionsCount

                if (groupIndex < currentGroupIndex) {
                    // Предыдущие группы полностью завершены
                    completedSteps += actionsCount
                } else if (groupIndex == currentGroupIndex) {
                    // Текущая группа
                    completedSteps += currentActionIndex
                }
            }

            return if (totalSteps > 0) {
                completedSteps.toFloat() / totalSteps
            } else {
                0f
            }
        }
}