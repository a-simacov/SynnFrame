package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.TaskFactLine

/**
 * События для экрана деталей задания
 */
sealed class TaskDetailEvent {

    data object NavigateBack : TaskDetailEvent()

    data object NavigateToProductsList : TaskDetailEvent()

    data class ShowSnackbar(val message: String) : TaskDetailEvent()

    data object ShowScanDialog : TaskDetailEvent()

    data class ShowFactLineDialog(val factLine: TaskFactLine) : TaskDetailEvent()

    data object CloseDialog : TaskDetailEvent()

    data object UpdateSuccess : TaskDetailEvent()

    data object ShowDeleteConfirmation : TaskDetailEvent()

    data object HideDeleteConfirmation : TaskDetailEvent()

    data object ShowProductSelectionDialog : TaskDetailEvent()

    data object HideProductSelectionDialog : TaskDetailEvent()

    data class UpdateProductFilter(val filter: String) : TaskDetailEvent()
}