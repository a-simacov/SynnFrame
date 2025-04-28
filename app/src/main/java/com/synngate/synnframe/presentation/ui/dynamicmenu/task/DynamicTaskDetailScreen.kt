package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.InfoCard
import com.synngate.synnframe.presentation.common.scaffold.InfoRow
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailEvent
import com.synngate.synnframe.util.html.HtmlUtils

@Composable
fun DynamicTaskDetailScreen(
    viewModel: DynamicTaskDetailViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicTaskDetailEvent.NavigateBack -> navigateBack()
                is DynamicTaskDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DynamicTaskDetailEvent.StartTaskExecution -> {
                    // В будущем добавим переход к экрану выполнения задания
                }
            }
        }
    }

    val taskName = state.task?.name?.let { HtmlUtils.stripHtml(it) } ?: ""

    AppScaffold(
        title = stringResource(id = R.string.task_details),
        subtitle = taskName, // Для заголовка используем текст без HTML-разметки
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            state.task?.let { task ->
                InfoCard {
                    InfoRow(
                        label = stringResource(id = R.string.task_id),
                        value = task.id
                    )

                    Text(
                        text = HtmlUtils.htmlToAnnotatedString(task.name),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ActionButton(
                    text = stringResource(id = R.string.start_task_execution),
                    onClick = viewModel::onStartTaskExecution,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}