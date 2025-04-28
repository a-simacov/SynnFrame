package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState

/**
 * Компонент для отображения списка заданий
 */
class TaskListComponent(
    private val state: DynamicTasksState,
    private val events: DynamicTasksScreenEvents
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Box(modifier = modifier) {
            if (state.tasks.isEmpty() && !state.isLoading) {
                Text(
                    text = if (state.error == null) {
                        stringResource(id = R.string.no_tasks_available)
                    } else {
                        formatErrorMessage(state.error)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                TasksList(
                    tasks = state.tasks,
                    onTaskClick = events.onTaskClick
                )
            }
        }
    }

    // Переопределяем функцию usesWeight для указания, что компонент должен использовать вес
    override fun usesWeight(): Boolean = true

    // Переопределяем функцию getWeight для указания веса компонента
    override fun getWeight(): Float = 1f

    @Composable
    private fun TasksList(
        tasks: List<DynamicTask>,
        onTaskClick: (DynamicTask) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun TaskItem(
        task: DynamicTask,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.task_id_fmt, task.id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Расширение реестра компонентов для регистрации компонента списка заданий
 */
fun ScreenComponentRegistry.registerTaskListComponent() {
    registerComponent(ScreenElementType.SHOW_LIST) { state ->
        TaskListComponent(state, events)
    }
}