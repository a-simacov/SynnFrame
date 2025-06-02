package com.synngate.synnframe.presentation.ui.dynamicmenu.task.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.common.status.TaskStatusVerticalBar
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenComponent
import com.synngate.synnframe.util.html.HtmlUtils

class TaskListComponent<S>(
    private val state: S,
    private val tasks: List<DynamicTask>,
    private val isLoading: Boolean,
    private val error: String?,
    private val onTaskClick: (DynamicTask) -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Box(modifier = modifier) {
            if (tasks.isEmpty() && !isLoading) {
                Text(
                    text = if (error == null) {
                        stringResource(id = R.string.no_tasks_available)
                    } else {
                        formatErrorMessage(error)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                TasksList(
                    tasks = tasks,
                    onTaskClick = onTaskClick
                )
            }
        }
    }

    override fun usesWeight(): Boolean = true

    override fun getWeight(): Float = 1f

    @Composable
    private fun TasksList(
        tasks: List<DynamicTask>,
        onTaskClick: (DynamicTask) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task) }
                )

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }

    @Composable
    private fun TaskItem(
        task: DynamicTask,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val annotatedName = HtmlUtils.htmlToAnnotatedString(task.name)
        val taskStatus = task.getTaskStatus()

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        text = HtmlUtils.htmlToAnnotatedString(task.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TaskStatusVerticalBar(
                    status = task.getTaskStatus(),
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }

    private fun formatErrorMessage(errorMessage: String?): String {
        if (errorMessage == null) return ""

        return errorMessage
            .replace("\n", ". ")
            .replace("..", ".")
            .replace(". .", ".")
    }
}