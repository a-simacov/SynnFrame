package com.synngate.synnframe.presentation.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.main.model.MainMenuEvent

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    navigateToTasks: () -> Unit,
    navigateToProducts: () -> Unit,
    navigateToLogs: () -> Unit,
    navigateToSettings: () -> Unit,
    navigateToLogin: () -> Unit,
    navigateToTasksX: () -> Unit,
    navigateToDynamicMenu: () -> Unit,
    exitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MainMenuEvent.NavigateToTasks -> navigateToTasks()
                is MainMenuEvent.NavigateToProducts -> navigateToProducts()
                is MainMenuEvent.NavigateToLogs -> navigateToLogs()
                is MainMenuEvent.NavigateToSettings -> navigateToSettings()
                is MainMenuEvent.NavigateToLogin -> navigateToLogin()
                is MainMenuEvent.ExitApp -> exitApp()
                is MainMenuEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is MainMenuEvent.NavigateToTasksX -> navigateToTasksX()
                is MainMenuEvent.ShowExitConfirmation -> {
                    // Обрабатывается через showExitConfirmation в state
                }
                is MainMenuEvent.NavigateToDynamicMenu -> navigateToDynamicMenu()
            }
        }
    }

    BackHandler {
        viewModel.onExitClick()
    }

    if (state.showExitConfirmation) {
        ConfirmationDialog(
            title = stringResource(id = R.string.exit_confirmation_title),
            message = stringResource(id = R.string.exit_confirmation_message),
            onConfirm = { viewModel.exitApp() },
            onDismiss = { viewModel.hideExitConfirmation() }
        )
    }

    AppScaffold(
        title = stringResource(id = R.string.main_menu_title),
        snackbarHostState = snackbarHostState,
        currentUser = state.currentUser?.name,
        isSyncing = state.isSyncing,
        lastSyncTime = state.lastSyncTime,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        menuItems = listOf(
            stringResource(id = R.string.refresh) to { viewModel.refreshData() },
            stringResource(id = R.string.sync_data) to { viewModel.syncData() }
        ),
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            NavigationButton(
                text = stringResource(id = R.string.tasks),
                onClick = { viewModel.onDynamicMenuClick() },
                icon = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = stringResource(id = R.string.operations)
            )

            Spacer(modifier = Modifier.height(12.dp))

//            NavigationButton(
//                text = stringResource(id = R.string.tasks),
//                onClick = { viewModel.onTasksClick() },
//                icon = Icons.AutoMirrored.Outlined.Assignment,
//                contentDescription = stringResource(id = R.string.tasks),
//                badge = state.assignedTasksCount.takeIf { it > 0 }
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            NavigationButton(
//                text = "Расширенные задания",
//                onClick = { viewModel.onTasksXClick() },
//                icon = Icons.AutoMirrored.Outlined.Assignment,
//                contentDescription = "Расширенные задания",
//                badge = null
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))

            NavigationButton(
                text = stringResource(id = R.string.products),
                onClick = { viewModel.onProductsClick() },
                icon = Icons.Outlined.Inventory,
                contentDescription = stringResource(id = R.string.products),
                badge = state.totalProductsCount.takeIf { it > 0 }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка "Логи"
            NavigationButton(
                text = stringResource(id = R.string.logs),
                onClick = { viewModel.onLogsClick() },
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                contentDescription = stringResource(id = R.string.logs)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка "Настройки"
            NavigationButton(
                text = stringResource(id = R.string.settings),
                onClick = { viewModel.onSettingsClick() },
                icon = Icons.Default.Settings,
                contentDescription = stringResource(id = R.string.settings)
            )

            // Кнопка синхронизации
            if (!state.isSyncing) {
                Spacer(modifier = Modifier.height(16.dp))

                ActionButton(
                    text = stringResource(id = R.string.sync_data),
                    onClick = { viewModel.syncData() },
                    icon = Icons.Default.Sync,
                    contentDescription = stringResource(id = R.string.sync_data),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.height(4.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка "Смена пользователя"
            NavigationButton(
                text = stringResource(id = R.string.change_user),
                onClick = { viewModel.onChangeUserClick() },
                icon = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.change_user)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка "Закрыть"
            NavigationButton(
                text = stringResource(id = R.string.exit),
                onClick = { viewModel.onExitClick() },
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(id = R.string.exit)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Информация о версии приложения
            Text(
                text = stringResource(
                    id = R.string.splash_version,
                    BuildConfig.VERSION_NAME
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}