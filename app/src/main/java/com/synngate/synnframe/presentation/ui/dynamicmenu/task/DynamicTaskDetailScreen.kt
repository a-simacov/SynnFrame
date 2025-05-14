package com.synngate.synnframe.presentation.ui.dynamicmenu.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.InfoCard
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.common.status.TaskXStatusIndicator
import com.synngate.synnframe.presentation.ui.dynamicmenu.task.model.DynamicTaskDetailEvent
import com.synngate.synnframe.util.html.HtmlUtils

@Composable
fun DynamicTaskDetailScreen(
    viewModel: DynamicTaskDetailViewModel,
    navigateBack: () -> Unit,
    navigateToTaskXDetail: (String) -> Unit,
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
                is DynamicTaskDetailEvent.NavigateToTaskXDetail -> {
                    // Переходим к экрану выполнения задания
                    navigateToTaskXDetail(event.taskId)
                }

                DynamicTaskDetailEvent.StartTaskExecution -> TODO()
            }
        }
    }

    val taskStatus = state.task?.getTaskStatus() ?: TaskXStatus.TO_DO

    AppScaffold(
        title = stringResource(id = R.string.task_details),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isLoading = state.isLoading
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.task_status),
                            modifier = Modifier.weight(0.3f)
                        )

                        TaskXStatusIndicator(
                            status = taskStatus
                        )
                    }

                    Text(
                        text = HtmlUtils.htmlToAnnotatedString(task.name),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (taskStatus == TaskXStatus.TO_DO) {
                    ActionButton(
                        text = stringResource(id = R.string.start_task_execution),
                        onClick = viewModel::onStartTaskExecution,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )
                }
            }

            if (state.task == null && state.error != null && !state.isLoading) {
                Text(
                    text = stringResource(id = R.string.error_loading_task_details),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 32.dp)
                )
            }
        }
    }
}