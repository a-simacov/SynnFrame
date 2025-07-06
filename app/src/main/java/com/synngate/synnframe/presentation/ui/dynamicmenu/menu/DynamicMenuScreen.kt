package com.synngate.synnframe.presentation.ui.dynamicmenu.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.model.DynamicMenuEvent

@Composable
fun DynamicMenuScreen(
    viewModel: DynamicMenuViewModel,
    navigateToDynamicTasks: (menuItemId: String, menuItemName: String, endpoint: String, screenSettings: ScreenSettings) -> Unit,
    navigateToDynamicProducts: (menuItemId: String, menuItemName: String, endpoint: String, screenSettings: ScreenSettings) -> Unit,
    navigateToCustomList: (menuItemId: String, menuItemName: String, endpoint: String, screenSettings: ScreenSettings) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // Создаем список кнопок навигации
    val navigationButtons = state.menuItems.mapIndexed { index, menuItem ->
        NavigationButtonData(
            text = menuItem.name,
            onClick = { viewModel.onMenuItemClick(menuItem) },
            icon = getIconForMenuItemType(menuItem.type),
            contentDescription = menuItem.name,
            keyCode = android.view.KeyEvent.KEYCODE_1 + index
        )
    }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicMenuEvent.NavigateToDynamicTasks -> {
                    navigateToDynamicTasks(
                        event.menuItemId,
                        event.menuItemName,
                        event.endpoint,
                        event.screenSettings
                    )
                }
                is DynamicMenuEvent.NavigateToDynamicProducts -> {
                    navigateToDynamicProducts(
                        event.menuItemId,
                        event.menuItemName,
                        event.endpoint,
                        event.screenSettings
                    )
                }
                is DynamicMenuEvent.NavigateToCustomList -> {
                    navigateToCustomList(
                        event.menuItemId,
                        event.menuItemName,
                        event.endpoint,
                        event.screenSettings
                    )
                }
                is DynamicMenuEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicMenuEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val onBackAction = {
        viewModel.onBackPressed()
    }

    BackHandler(enabled = state.currentMenuItemId != null) {
        viewModel.onBackPressed()
    }

    val title = if (state.currentMenuItemId == null) {
        stringResource(id = R.string.operations_menu_title)
    } else {
        val currentMenuItem = state.menuItems.firstOrNull { it.id == state.currentMenuItemId }
        currentMenuItem?.name ?: stringResource(id = R.string.submenu_title)
    }

    AppScaffold(
        title = title,
        onNavigateBack = onBackAction,
        actions = {
            IconButton(onClick = { viewModel.onRefresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = {
            viewModel.clearError()
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            if (state.menuItems.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(id = R.string.no_operations_available),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                DynamicMenuContent(
                    menuItems = state.menuItems,
                    onMenuItemClick = { menuItem ->
                        viewModel.onMenuItemClick(menuItem)
                    }
                )
            }
        }
    }
}

@Composable
private fun DynamicMenuContent(
    menuItems: List<DynamicMenuItem>,
    onMenuItemClick: (DynamicMenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        menuItems.forEachIndexed { index, menuItem ->
            val icon = getIconForMenuItemType(menuItem.type)

            NavigationButton(
                text = menuItem.name,
                onClick = { onMenuItemClick(menuItem) },
                icon = icon,
                contentDescription = menuItem.name,
                keyCode = index + 1
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class NavigationButtonData(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector,
    val contentDescription: String,
    val keyCode: Int
)

private fun handleKeyPress(keyCode: Int, navigationButtons: List<NavigationButtonData>) {
    navigationButtons.find { it.keyCode == keyCode }?.onClick?.invoke()
}

@Composable
private fun getIconForMenuItemType(type: DynamicMenuItemType): ImageVector {
    return when (type) {
        DynamicMenuItemType.SUBMENU -> Icons.Outlined.SubdirectoryArrowRight
        DynamicMenuItemType.TASKS -> Icons.AutoMirrored.Outlined.ListAlt
        DynamicMenuItemType.PRODUCTS -> Icons.Outlined.Inventory
        DynamicMenuItemType.CUSTOM_LIST -> Icons.AutoMirrored.Outlined.ListAlt
    }
}