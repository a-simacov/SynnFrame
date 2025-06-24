package com.synngate.synnframe.presentation.ui.main

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.main.model.MainMenuEvent
import com.synngate.synnframe.presentation.ui.main.model.MainMenuState

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
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
    val focusRequester = remember { FocusRequester() }

    val navigationButtons = getNavigationButtons(viewModel, state)

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
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
        isSyncing = state.isSyncing,
        currentUser = state.currentUser?.name,
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
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        handleKeyPress(event.key.nativeKeyCode, navigationButtons)
                        true
                    } else {
                        false
                    }
                }
        ) {
            navigationButtons.forEachIndexed { index, buttonData ->
                NavigationButton(
                    text = buttonData.text,
                    onClick = buttonData.onClick,
                    modifier = Modifier,
                    icon = buttonData.icon,
                    contentDescription = buttonData.contentDescription,
                    badge = buttonData.badge,
                    keyCode = index + 1
                )
                if (index < navigationButtons.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun getNavigationButtons(
    viewModel: MainMenuViewModel,
    state: MainMenuState
) = listOfNotNull(
    NavigationButtonData(
        text = stringResource(id = R.string.tasks),
        onClick = { viewModel.onDynamicMenuClick() },
        icon = Icons.AutoMirrored.Outlined.Assignment,
        contentDescription = stringResource(id = R.string.operations)
    ),
    NavigationButtonData(
        text = stringResource(id = R.string.products),
        onClick = { viewModel.onProductsClick() },
        icon = Icons.Outlined.Inventory,
        contentDescription = stringResource(id = R.string.products),
        badge = state.totalProductsCount.takeIf { it > 0 }
    ),
    NavigationButtonData(
        text = stringResource(id = R.string.settings),
        onClick = { viewModel.onSettingsClick() },
        icon = Icons.Default.Settings,
        contentDescription = stringResource(id = R.string.settings)
    ),
    NavigationButtonData(
        text = stringResource(id = R.string.change_user),
        onClick = { viewModel.onChangeUserClick() },
        icon = Icons.Default.Person,
        contentDescription = stringResource(id = R.string.change_user)
    ),
    NavigationButtonData(
        text = stringResource(id = R.string.logs),
        onClick = { viewModel.onLogsClick() },
        icon = Icons.AutoMirrored.Outlined.ListAlt,
        contentDescription = stringResource(id = R.string.logs)
    ),
    NavigationButtonData(
        text = stringResource(id = R.string.exit),
        onClick = { viewModel.onExitClick() },
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        contentDescription = stringResource(id = R.string.exit)
    )
).mapIndexed { index, buttonData ->
    buttonData.copy(keyCode = KeyEvent.KEYCODE_1 + index)
}

data class NavigationButtonData(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector,
    val contentDescription: String,
    val badge: Int? = null,
    val keyCode: Int? = null
)

private fun handleKeyPress(keyCode: Int, navigationButtons: List<NavigationButtonData>) {
    navigationButtons.find { it.keyCode == keyCode }?.onClick?.invoke()
}