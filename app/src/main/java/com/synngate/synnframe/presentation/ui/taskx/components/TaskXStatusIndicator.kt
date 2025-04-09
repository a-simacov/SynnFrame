// Индикатор статуса задания X
package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus

@Composable
fun TaskXStatusIndicator(
    status: TaskXStatus,
    formatStatus: (TaskXStatus) -> String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        TaskXStatus.TO_DO -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        TaskXStatus.IN_PROGRESS -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        TaskXStatus.PAUSED -> Pair(Color(0xFFFFF9C4), Color(0xFF616161)) // Light yellow, Gray
        TaskXStatus.COMPLETED -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        TaskXStatus.CANCELLED -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = formatStatus(status),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}