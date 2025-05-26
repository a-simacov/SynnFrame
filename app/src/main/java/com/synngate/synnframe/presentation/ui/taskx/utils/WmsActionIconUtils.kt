package com.synngate.synnframe.presentation.ui.taskx.utils

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.WmsAction

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