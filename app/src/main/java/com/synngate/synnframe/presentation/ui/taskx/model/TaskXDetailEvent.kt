package com.synngate.synnframe.presentation.ui.taskx.model

sealed class TaskXDetailEvent {
    // Управление заданием
    object StartTask : TaskXDetailEvent()
    object PauseTask : TaskXDetailEvent()
    object ResumeTask : TaskXDetailEvent()
    object CompleteTask : TaskXDetailEvent()

    // Управление действиями
    data class SelectAction(val actionId: String) : TaskXDetailEvent()
    data class ToggleActionStatus(val actionId: String) : TaskXDetailEvent()

    // Поиск
    data class UpdateSearchQuery(val query: String) : TaskXDetailEvent()
    object SearchActions : TaskXDetailEvent()
    object ClearSearch : TaskXDetailEvent()
    object ToggleSearchField : TaskXDetailEvent()
    object ShowCameraScanner : TaskXDetailEvent()
    object HideCameraScanner : TaskXDetailEvent()
    data class SearchByBarcode(val barcode: String) : TaskXDetailEvent()

    // Фильтрация
    data class ChangeDisplayMode(val mode: ActionDisplayMode) : TaskXDetailEvent()
    object FilterByBuffer : TaskXDetailEvent()
    object ClearFilters : TaskXDetailEvent()

    // Диалоги
    object ShowActionsDialog : TaskXDetailEvent()
    object HideActionsDialog : TaskXDetailEvent()
    object ShowCompletionDialog : TaskXDetailEvent()
    object HideCompletionDialog : TaskXDetailEvent()
    object HideInitialActionsDialog : TaskXDetailEvent()

    // Навигация
    object NavigateBack : TaskXDetailEvent()
    object HandleBackPress : TaskXDetailEvent()

    // Буфер
    object ToggleBufferPanel : TaskXDetailEvent()
    data class RemoveBufferItem(val itemId: String) : TaskXDetailEvent()
}