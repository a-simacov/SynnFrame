// Файл: com.synngate.synnframe.presentation.ui.logs.LogDetailState.kt

package com.synngate.synnframe.presentation.ui.logs.model

import com.synngate.synnframe.domain.entity.Log

/**
 * Состояние экрана деталей лога
 */
data class LogDetailState(
    // Данные лога
    val log: Log? = null,

    // Статус загрузки
    val isLoading: Boolean = true,

    // Сообщение об ошибке
    val error: String? = null,

    // Флаг успешного копирования в буфер обмена
    val isTextCopied: Boolean = false,

    // Флаг подтверждения удаления
    val showDeleteConfirmation: Boolean = false,

    // Статус выполнения операции удаления
    val isDeletingLog: Boolean = false
)