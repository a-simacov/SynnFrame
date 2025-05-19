package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.TaskX

@Composable
fun TaskProgressIndicator(
    task: TaskX,
    modifier: Modifier = Modifier
) {
    val regularActions = task.plannedActions.filter { !it.isFinalAction && !it.isInitialAction }
    val finalActions = task.plannedActions.filter { it.isFinalAction }
    val initialActions = task.plannedActions.filter { it.isInitialAction }

    val completedRegularActions = regularActions.count { it.isCompleted }
    val completedFinalActions = finalActions.count { it.isCompleted }
    val completedInitialActions = initialActions.count { it.isCompleted }

    val totalProgress = if (task.plannedActions.isNotEmpty()) {
        task.plannedActions.count { it.isCompleted }.toFloat() / task.plannedActions.size
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

            // Отдельный индикатор прогресса для начальных действий, если они есть
            if (initialActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                val initialProgress = if (initialActions.isNotEmpty()) {
                    completedInitialActions.toFloat() / initialActions.size
                } else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Начальные:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.width(80.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LinearProgressIndicator(
                        progress = { initialProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}