package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType

@Composable
fun TaskProgressIndicator(
    task: TaskX,
    modifier: Modifier = Modifier
) {
    val regularActions =
        task.plannedActions.filter { it.completionOrderType != CompletionOrderType.FINAL && it.completionOrderType != CompletionOrderType.INITIAL }
    val finalActions = task.plannedActions.filter { it.completionOrderType == CompletionOrderType.FINAL }
    val initialActions = task.plannedActions.filter { it.completionOrderType == CompletionOrderType.INITIAL }

    val completedRegularActions = regularActions.count { it.isFullyCompleted(task.factActions) }
    val completedFinalActions = finalActions.count { it.isFullyCompleted(task.factActions) }
    val completedInitialActions = initialActions.count { it.isFullyCompleted(task.factActions) }

    val totalProgress = if (task.plannedActions.isNotEmpty()) {
        task.plannedActions.count { it.isFullyCompleted(task.factActions) }.toFloat() / task.plannedActions.size
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Общий прогресс выполнения задания
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("$completedRegularActions/${regularActions.size}")

                        if (initialActions.isNotEmpty()) {
                            append(" (🚀 $completedInitialActions/${initialActions.size})")
                        }

                        if (finalActions.isNotEmpty()) {
                            append(" (⚡ $completedFinalActions/${finalActions.size})")
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                LinearProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}