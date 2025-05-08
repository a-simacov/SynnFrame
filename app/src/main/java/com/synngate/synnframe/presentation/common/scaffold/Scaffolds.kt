package com.synngate.synnframe.presentation.common.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.AppInsetsConfigHolder
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.presentation.common.LocalCurrentUser
import com.synngate.synnframe.presentation.common.status.NotificationBar
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.common.status.SyncStatusIndicator
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isSyncing: Boolean = false,
    lastSyncTime: String? = null,
    currentUser: String? = null,
    notification: Pair<String, StatusType>? = null,
    onDismissNotification: (() -> Unit)? = null,
    drawerState: DrawerState? = null,
    drawerContent: @Composable (() -> Unit)? = null,
    menuItems: List<Pair<String, () -> Unit>>? = null,
    isLoading: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val windowInsets = WindowInsets.systemBars

    // Локальное состояние видимости уведомления
    var isNotificationVisible by remember(notification) { mutableStateOf(notification != null) }

    // Получение данных о текущем пользователе через CompositionLocal
    val localCurrentUser = LocalCurrentUser.current
    val finalUserName = currentUser ?: localCurrentUser?.name

    // Получаем состояние синхронизации из ApplicationContext
    val context = LocalContext.current
    val app = remember { context.applicationContext as SynnFrameApplication }

    // Следим за состоянием синхронизации
    val syncController = remember { app.appContainer.synchronizationController }
    val lastSyncInfo by syncController.lastSyncInfo.collectAsState(initial = null)

    // Определяем, выполняется ли синхронизация и время последней синхронизации
    val finalIsSyncing = isSyncing
    val finalLastSyncTime = lastSyncTime ?:
    lastSyncInfo?.timestamp?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    // Обновляем видимость уведомления при изменении параметра notification
    LaunchedEffect(notification) {
        isNotificationVisible = notification != null
    }

    // Получаем системные инсеты
    val topInsetDp = AppInsetsConfigHolder.topInsetDp
    val topAppBarInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top).add(
        WindowInsets(top = topInsetDp.dp)
    )

    val mainContent = @Composable {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column(
                                modifier = Modifier.padding(vertical = 0.dp) // Уменьшенные вертикальные отступы
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.back)
                                    )
                                }
                            } else if (drawerState != null) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isOpen) {
                                                drawerState.close()
                                            } else {
                                                drawerState.open()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            }
                        },
                        actions = {
                            actions()

                            if (!menuItems.isNullOrEmpty()) {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options"
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    menuItems.forEach { (text, action) ->
                                        DropdownMenuItem(
                                            text = { Text(text) },
                                            onClick = {
                                                menuExpanded = false
                                                action()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.padding(vertical = 0.dp),//.height(64.dp),
                        windowInsets = topAppBarInsets
                    )

                    notification?.let { (message, type) ->
                        NotificationBar(
                            visible = isNotificationVisible,
                            message = message,
                            type = type,
                            onDismiss = {
                                isNotificationVisible = false
                                onDismissNotification?.invoke()
                            }
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .navigationBarsPadding()
                    ) {
                        bottomBar()

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            finalUserName?.let {
                                Text(
                                    text = "Пользователь: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }

                            SyncStatusIndicator(
                                isSyncing = finalIsSyncing,
                                lastSyncTime = finalLastSyncTime,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = floatingActionButton,
            contentWindowInsets = windowInsets
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                content(paddingValues)

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Отображаем с боковым меню или без него
    if (drawerState != null && drawerContent != null) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    drawerContent()
                }
            }
        ) {
            mainContent()
        }
    } else {
        mainContent()
    }
}

/**
 * Метод расширения для WindowInsets, который добавляет другие инсеты
 * Правильно обрабатывает конвертацию между пикселями и DP
 */
@Composable
fun WindowInsets.add(insets: WindowInsets): WindowInsets {
    return WindowInsets.add(this, insets)
}

/**
 * Статический метод для WindowInsets.Companion, который добавляет инсеты
 */
@Composable
fun WindowInsets.Companion.add(first: WindowInsets, second: WindowInsets): WindowInsets {
    // Использует встроенные методы и конвертеры, чтобы правильно складывать инсеты
    return WindowInsets(
        left = first.getLeft(LocalDensity.current, LocalLayoutDirection.current).dp +
                second.getLeft(LocalDensity.current, LocalLayoutDirection.current).dp,
        top = first.getTop(LocalDensity.current).dp +
                second.getTop(LocalDensity.current).dp,
        right = first.getRight(LocalDensity.current, LocalLayoutDirection.current).dp +
                second.getRight(LocalDensity.current, LocalLayoutDirection.current).dp,
        bottom = first.getBottom(LocalDensity.current).dp +
                second.getBottom(LocalDensity.current).dp
    )
}

/**
 * Компонент для отображения пустого экрана с сообщением
 */
@Composable
fun EmptyScreenContent(
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon?.invoke()

            if (icon != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Компонент для отображения экрана загрузки
 */
@Composable
fun LoadingScreenContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Компонент для отображения экрана с ошибкой
 */
@Composable
fun ErrorScreenContent(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.error),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.Button(
                    onClick = onRetry
                ) {
                    Text(stringResource(id = R.string.retry))
                }
            }
        }
    }
}

@Composable
fun ScrollableScreenContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            content()
        }
    }
}