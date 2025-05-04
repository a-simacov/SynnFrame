package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun TaskProgressIndicator(
    task: TaskX,
    modifier: Modifier = Modifier
) {
    val regularActions = task.plannedActions.filter { !it.isFinalAction }
    val finalActions = task.plannedActions.filter { it.isFinalAction }

    val completedRegularActions = regularActions.count { it.isCompleted }
    val completedFinalActions = finalActions.count { it.isCompleted }

    val totalProgress = if (task.plannedActions.isNotEmpty()) {
        task.plannedActions.count { it.isCompleted }.toFloat() / task.plannedActions.size
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$completedRegularActions/${regularActions.size} " +
                        (if (finalActions.isNotEmpty()) " (⚡ $completedFinalActions/${finalActions.size})" else ""),
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