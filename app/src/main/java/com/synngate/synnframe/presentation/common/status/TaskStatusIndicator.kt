package com.synngate.synnframe.presentation.common.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus

@Composable
fun TaskStatusVerticalBar(status: TaskXStatus, modifier: Modifier = Modifier) {
    val color = getStatusColor(status)

    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(color)
    )
}

@Composable
fun getStatusColor(status: TaskXStatus): Color {
    return when (status) {
        TaskXStatus.TO_DO -> Color(0xFF2196F3)         // Синий
        TaskXStatus.IN_PROGRESS -> Color(0xFF4CAF50)   // Зеленый
        TaskXStatus.PAUSED -> Color(0xFFFFC107)        // Желтый/Оранжевый
        TaskXStatus.COMPLETED -> Color(0xFF9E9E9E)     // Серый
        TaskXStatus.CANCELLED -> Color(0xFFF44336)     // Красный
    }
}