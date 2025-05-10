package com.synngate.synnframe.presentation.ui.taskx.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Output
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.synngate.synnframe.domain.entity.taskx.WmsAction

/**
 * Получает иконку для соответствующего типа действия WMS
 */
@Composable
fun getWmsActionIcon(action: WmsAction): ImageVector {
    return when (action) {
        WmsAction.PUT_INTO -> Icons.AutoMirrored.Filled.Input      // Положить
        WmsAction.TAKE_FROM -> Icons.Default.Output    // Взять
        WmsAction.RECEIPT -> Icons.Default.Archive     // Оприходовать
        WmsAction.EXPENSE -> Icons.Default.Delete      // Списать
        WmsAction.RECOUNT -> Icons.Default.Loop        // Пересчитать
        WmsAction.USE -> Icons.Default.Handyman        // Использовать
        WmsAction.ASSERT -> Icons.Default.CheckCircle  // Подтверждение
    }
}

/**
 * Получает описание для соответствующего типа действия WMS
 */
@Composable
fun getWmsActionDescription(action: WmsAction): String {
    return when (action) {
        WmsAction.PUT_INTO -> "Положить"
        WmsAction.TAKE_FROM -> "Взять"
        WmsAction.RECEIPT -> "Оприходовать"
        WmsAction.EXPENSE -> "Списать"
        WmsAction.RECOUNT -> "Пересчитать"
        WmsAction.USE -> "Использовать"
        WmsAction.ASSERT -> "Подтверждение"
    }
}