package com.synngate.synnframe.presentation.ui.tasks.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.presentation.common.status.TaskStatusIndicator
import java.time.format.DateTimeFormatter

/**
 * Элемент списка для отображения задания
 */
@Composable
fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when(task.type) {
                            com.synngate.synnframe.domain.entity.TaskType.RECEIPT -> stringResource(R.string.task_type_receipt)
                            com.synngate.synnframe.domain.entity.TaskType.PICK -> stringResource(R.string.task_type_pick)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TaskStatusIndicator(
                    status = when(task.status) {
                        com.synngate.synnframe.domain.entity.TaskStatus.TO_DO -> stringResource(R.string.task_status_to_do)
                        com.synngate.synnframe.domain.entity.TaskStatus.IN_PROGRESS -> stringResource(R.string.task_status_in_progress)
                        com.synngate.synnframe.domain.entity.TaskStatus.COMPLETED -> stringResource(R.string.task_status_completed)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.created_at, task.createdAt.format(dateFormatter)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                val lastModifiedAt = task.completedAt ?: task.startedAt ?: task.viewedAt ?: task.createdAt
                Text(
                    text = stringResource(R.string.last_modified_at, lastModifiedAt.format(dateFormatter)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Дополнительная информация о выполнении
            if (task.status == com.synngate.synnframe.domain.entity.TaskStatus.IN_PROGRESS ||
                task.status == com.synngate.synnframe.domain.entity.TaskStatus.COMPLETED) {

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val completionPercent = task.getCompletionPercentage()
                    Text(
                        text = stringResource(R.string.task_completion, completionPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (task.uploaded) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.task_uploaded),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}