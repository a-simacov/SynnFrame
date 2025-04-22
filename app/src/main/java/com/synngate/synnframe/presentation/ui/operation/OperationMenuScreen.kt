package com.synngate.synnframe.presentation.ui.operation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.OperationMenuType
import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.operation.model.OperationMenuEvent

@Composable
fun OperationMenuScreen(
    viewModel: OperationMenuViewModel,
    navigateToOperationTasks: (operationId: String, operationName: String, operationType: OperationMenuType) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OperationMenuEvent.NavigateToOperationTasks -> {
                    navigateToOperationTasks(event.operationId, event.operationName, event.operationType)
                }
                is OperationMenuEvent.NavigateBack -> {
                    navigateBack()
                }
                is OperationMenuEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    AppScaffold(
        title = stringResource(id = R.string.operations_menu_title),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        actions = {
            IconButton(onClick = { viewModel.onRefresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.operations.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_operations_available),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                OperationMenuContent(
                    operations = state.operations,
                    onOperationClick = { operation ->
                        viewModel.onOperationClick(operation.id, operation.name, operation.type)
                    }
                )
            }
        }
    }
}

@Composable
private fun OperationMenuContent(
    operations: List<OperationMenuItem>,
    onOperationClick: (OperationMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.available_operations),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        operations.forEach { operation ->
            NavigationButton(
                text = operation.name,
                onClick = { onOperationClick(operation) },
                icon = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = operation.name
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}