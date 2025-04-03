package com.synngate.synnframe.presentation.ui.logs.model

import com.synngate.synnframe.domain.entity.Log

/**
 * Состояние экрана деталей лога
 */
data class LogDetailState(

    val log: Log? = null,

    val isLoading: Boolean = true,

    val error: String? = null,

    val isTextCopied: Boolean = false,

    val isDeleteConfirmationVisible: Boolean = false,

    val isDeletingLog: Boolean = false
)