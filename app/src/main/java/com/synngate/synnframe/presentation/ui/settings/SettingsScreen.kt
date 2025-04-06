package com.synngate.synnframe.presentation.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.buttons.CarouselValueButton
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.buttons.PropertyToggleButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.dialog.ProgressDialog
import com.synngate.synnframe.presentation.common.inputs.NumberTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.InfoCard
import com.synngate.synnframe.presentation.common.scaffold.ScrollableScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.settings.model.SettingsEvent
import com.synngate.synnframe.presentation.ui.settings.model.SettingsState
import com.synngate.synnframe.util.logging.LogLevel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navigateToServerList: () -> Unit,
    navigateToSyncHistory: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.downloadUpdate()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Для установки обновления требуется разрешение"
                )
            }
        }
    }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is SettingsEvent.NavigateBack -> {
                    navigateBack()
                }

                is SettingsEvent.NavigateToServerList -> {
                    navigateToServerList()
                }

                is SettingsEvent.SettingsUpdated -> {
                    // Опционально можно показать сообщение об успешном обновлении
                }

                is SettingsEvent.RequestInstallPermission -> {
                    // Запускаем Intent для запроса разрешения на установку
                    event.intent?.let { intent ->
                        installPermissionLauncher.launch(intent)
                    }
                }

                is SettingsEvent.InstallUpdate -> {
                    // Запускаем Intent для установки обновления
                    try {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                event.uri,
                                "application/vnd.android.package-archive"
                            )
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        Timber.e(e, "Error starting install intent")
                        snackbarHostState.showSnackbar("Ошибка установки: ${e.message}")
                    }
                }

                is SettingsEvent.NavigateToSyncHistory -> navigateToSyncHistory()

                is SettingsEvent.ChangeAppLanguage -> {
                    // Изменяем локаль приложения
                    val locale = Locale(event.languageCode)
                    val resources = context.resources
                    val configuration = resources.configuration
                    configuration.setLocale(locale)
                    resources.updateConfiguration(configuration, resources.displayMetrics)

                    // Перезапускаем активность для применения изменений
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                }
            }
        }
    }

    val showUpdateConfirmDialog by remember(state.showUpdateConfirmDialog, state.lastVersion) {
        derivedStateOf { state.showUpdateConfirmDialog && state.lastVersion != null }
    }

    val isLoading by remember(
        state.isCheckingForUpdates,
        state.isDownloadingUpdate,
        state.isInstallingUpdate
    ) {
        derivedStateOf {
            state.isCheckingForUpdates || state.isDownloadingUpdate || state.isInstallingUpdate
        }
    }

    if (showUpdateConfirmDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.update_available),
            message = stringResource(
                id = R.string.update_dialog_message,
                state.lastVersion ?: "",
                state.releaseDate ?: ""
            ),
            onConfirm = { viewModel.downloadUpdate() },
            onDismiss = { viewModel.hideUpdateConfirmDialog() }
        )
    }

    if (isLoading) {
        val message = when {
            state.isCheckingForUpdates -> stringResource(id = R.string.checking_for_updates)
            state.isDownloadingUpdate -> stringResource(id = R.string.downloading_update)
            state.isInstallingUpdate -> stringResource(id = R.string.installing_update)
            else -> ""
        }
        if (message.isNotEmpty()) {
            ProgressDialog(message = message)
        }
    }

    AppScaffold(
        title = stringResource(id = R.string.settings_title),
        snackbarHostState = snackbarHostState,
        onNavigateBack = navigateBack,
        notification = state.error?.let { Pair(it, StatusType.ERROR) }
    ) { paddingValues ->
        ScrollableScreenContent(
            modifier = modifier.padding(paddingValues)
        ) {
            ActiveServerSection(
                state = state,
                onNavigateToServerList = navigateToServerList,
                onShowServersOnStartupChange = viewModel::updateShowServersOnStartup,
                onPeriodicUploadEnabledChange = viewModel::updatePeriodicUpload,
                onUploadIntervalChange = viewModel::updateUploadInterval
            )

            Spacer(modifier = Modifier.height(16.dp))

            WebServerSection(
                state = state,
                onToggleWebServer = viewModel::toggleWebServer
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoggingSettingsSection(
                state = state,
                onLogLevelChange = viewModel::updateLogLevel,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SynchronizationSection(
                state = state,
                onToggleSyncService = viewModel::toggleSyncService,
                onStartManualSync = viewModel::startManualSync,
                onUpdatePeriodicSync = viewModel::updatePeriodicSync,
                onSyncTaskTypes = viewModel::syncTaskTypes,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            NavigationButton(
                text = stringResource(id = R.string.sync_history),
                onClick = { viewModel.onSyncHistoryClick() },
                icon = Icons.Filled.History,
                contentDescription = stringResource(id = R.string.sync_history)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Оформление интерфейса
            InterfaceSettingsSection(
                state = state,
                onThemeModeChange = viewModel::updateThemeMode,
                onLanguageCodeChange = viewModel::updateLanguageCode,
                onNavigationButtonHeightChange = viewModel::updateNavigationButtonHeight
            )

            Spacer(modifier = Modifier.height(16.dp))

            BinPatternSection(
                state = state,
                onUpdateBinCodePattern = viewModel::updateBinCodePattern,
                modifier = Modifier.fillMaxWidth()
            )

            // Обновление
            UpdateSection(
                state = state,
                onCheckForUpdates = viewModel::checkForUpdates,
                onShowUpdateConfirmDialog = viewModel::showUpdateConfirmDialog,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }
}

@Stable
@Composable
fun ActiveServerSection(
    state: SettingsState,
    onNavigateToServerList: () -> Unit,
    onShowServersOnStartupChange: (Boolean) -> Unit,
    onPeriodicUploadEnabledChange: (Boolean) -> Unit,
    onUploadIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Оптимизация: кэшируем значения, чтобы избежать лишних вычислений
    val periodicUploadEnabled = remember(state.periodicUploadEnabled) { state.periodicUploadEnabled }

    InfoCard(
        title = stringResource(id = R.string.active_server_settings),
        modifier = modifier
    ) {
        NavigationButton(
            text = stringResource(id = R.string.navigate_to_servers),
            onClick = onNavigateToServerList,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.activeServer != null) {
            val server = state.activeServer
            Text(
                text = stringResource(id = R.string.active_server_info, server.name),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = stringResource(
                    id = R.string.server_host_port_info,
                    server.host,
                    server.port
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = stringResource(id = R.string.no_active_server),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))
        PropertyToggleButton(
            property = stringResource(id = R.string.server_show_on_startup),
            value = state.showServersOnStartup,
            onToggle = onShowServersOnStartupChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        PropertyToggleButton(
            property = stringResource(id = R.string.periodic_upload_enabled),
            value = state.periodicUploadEnabled,
            onToggle = onPeriodicUploadEnabledChange,
            modifier = Modifier.fillMaxWidth()
        )

        // Оптимизация: AnimatedVisibility с condition в remember
        val showIntervalSettings = remember(periodicUploadEnabled) { periodicUploadEnabled }
        AnimatedVisibility(visible = showIntervalSettings) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.upload_interval_seconds),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NumberTextField(
                        value = state.uploadIntervalSeconds.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { onUploadIntervalChange(it) }
                        },
                        label = stringResource(id = R.string.interval_seconds),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.interval_range_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SynchronizationSection(
    state: SettingsState,
    onToggleSyncService: () -> Unit,
    onStartManualSync: () -> Unit,
    onSyncTaskTypes: () -> Unit,
    onUpdatePeriodicSync: (Boolean, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.sync_settings),
        modifier = modifier
    ) {
        // Статус сервиса синхронизации
        Text(
            text = stringResource(
                id = if (state.isSyncServiceRunning)
                    R.string.sync_service_running
                else
                    R.string.sync_service_stopped
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.isSyncServiceRunning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Кнопка управления сервисом синхронизации
        ActionButton(
            text = stringResource(
                id = if (state.isSyncServiceRunning)
                    R.string.stop_sync_service
                else
                    R.string.start_sync_service
            ),
            onClick = onToggleSyncService,
            modifier = Modifier.fillMaxWidth(),
            buttonColors = if (state.isSyncServiceRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка для ручной синхронизации
        ActionButton(
            text = stringResource(id = R.string.start_manual_sync),
            onClick = onStartManualSync,
            isLoading = state.isManualSyncing,
            enabled = !state.isManualSyncing && state.syncStatus != SynchronizationController.SyncStatus.SYNCING,
            modifier = Modifier.fillMaxWidth()
        )

        // Информация о последней синхронизации
        if (state.lastSyncInfo != null) {
            val lastSync = state.lastSyncInfo
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.last_sync_info),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = stringResource(
                    id = R.string.last_sync_time,
                    lastSync.timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(
                    id = R.string.sync_results,
                    lastSync.tasksUploadedCount,
                    lastSync.tasksDownloadedCount,
                    lastSync.productsDownloadedCount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Настройка периодической синхронизации
        PropertyToggleButton(
            property = stringResource(id = R.string.periodic_sync_enabled),
            value = state.periodicSyncEnabled,
            onToggle = { enabled -> onUpdatePeriodicSync(enabled, null) },
            modifier = Modifier.fillMaxWidth()
        )

        // В SynchronizationSection добавляем еще одну кнопку
        Spacer(modifier = Modifier.height(16.dp))

        ActionButton(
            text = stringResource(id = R.string.sync_task_types),
            onClick = onSyncTaskTypes,
            isLoading = state.isSyncingTaskTypes,
            enabled = !state.isSyncingTaskTypes && state.syncStatus != SynchronizationController.SyncStatus.SYNCING,
            modifier = Modifier.fillMaxWidth()
        )

// В случае отдельного информационного раздела
        if ((state.lastSyncInfo?.taskTypesDownloadedCount ?: 0) > 0) {
            Text(
                text = stringResource(
                    id = R.string.task_types_downloaded,
                    state.lastSyncInfo?.taskTypesDownloadedCount ?: 0
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Настройка интервала синхронизации
        AnimatedVisibility(visible = state.periodicSyncEnabled) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.sync_interval_seconds),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NumberTextField(
                        value = state.syncIntervalSeconds.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let {
                                onUpdatePeriodicSync(true, it)
                            }
                        },
                        label = stringResource(id = R.string.interval_seconds),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.interval_range_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WebServerSection(
    state: SettingsState,
    onToggleWebServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.web_server_settings),
        modifier = modifier
    ) {
        // Текст с состоянием веб-сервера
        Text(
            text = stringResource(
                id = if (state.isWebServerRunning)
                    R.string.web_server_running
                else
                    R.string.web_server_stopped
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.isWebServerRunning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Кнопка управления веб-сервером
        ActionButton(
            text = stringResource(
                id = if (state.isWebServerRunning)
                    R.string.stop_web_server
                else
                    R.string.start_web_server
            ),
            onClick = onToggleWebServer,
            modifier = Modifier.fillMaxWidth(),
            buttonColors = if (state.isWebServerRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun InterfaceSettingsSection(
    state: SettingsState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageCodeChange: (String) -> Unit,
    onNavigationButtonHeightChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.interface_settings),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.theme_selection),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CarouselValueButton(
            values = ThemeMode.entries,
            currentValue = state.themeMode,
            onValueChange = onThemeModeChange,
            valueToString = { theme ->
                when(theme) {
                    ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                    ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                }
            },
            //labelText = stringResource(id = R.string.theme),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.language_selection),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CarouselValueButton(
            values = listOf("ru", "en"),
            currentValue = state.languageCode,
            onValueChange = onLanguageCodeChange,
            valueToString = { code ->
                when(code) {
                    "ru" -> "Русский"
                    "en" -> "English"
                    else -> code
                }
            },
            //labelText = stringResource(id = R.string.language),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Настройка высоты кнопки навигации
        Text(
            text = stringResource(id = R.string.button_height_setting),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Слайдер для выбора высоты
        // Слайдер с улучшенной доступностью
        val sliderPosition = remember(state.navigationButtonHeight) { state.navigationButtonHeight }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        id = R.string.button_height_value,
                        sliderPosition.toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "dp",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderPosition,
                onValueChange = onNavigationButtonHeightChange,
                valueRange = 48f..96f,
                steps = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Высота кнопки: ${sliderPosition.toInt()} dp"
                    }
            )

            // Подсказка по диапазону
            Text(
                text = stringResource(id = R.string.button_height_range_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BinPatternSection(
    state: SettingsState,
    onUpdateBinCodePattern: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(R.string.bin_settings),
        modifier = modifier
    ) {

        // Шаблон кода ячейки
        OutlinedTextField(
            value = state.binCodePattern,
            onValueChange = { onUpdateBinCodePattern(it) },
            label = { Text(stringResource(R.string.bin_code_pattern)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
fun UpdateSection(
    state: SettingsState,
    onCheckForUpdates: () -> Unit,
    onShowUpdateConfirmDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.update_settings),
        modifier = modifier
    ) {
        // Информация о текущей версии
        Text(
            text = stringResource(
                id = R.string.current_version,
                BuildConfig.VERSION_NAME
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Информация о доступной версии, если есть
        if (state.lastVersion != null) {
            Text(
                text = stringResource(
                    id = R.string.available_version,
                    state.lastVersion
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (state.releaseDate != null) {
                Text(
                    text = stringResource(
                        id = R.string.release_date,
                        state.releaseDate
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // Индикатор прогресса загрузки
        AnimatedVisibility(
            visible = state.isDownloadingUpdate,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.downloading_update),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LinearProgressIndicator(
                    progress = { state.downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Text(
                    text = "${state.downloadProgress}%",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка проверки обновлений
        ActionButton(
            text = stringResource(
                id = if (state.lastVersion != null)
                    R.string.install_update
                else
                    R.string.check_for_updates
            ),
            onClick = {
                if (state.lastVersion != null) {
                    // Если уже проверили и есть обновление, показываем диалог
                    onShowUpdateConfirmDialog()
                } else {
                    // Иначе проверяем наличие обновлений
                    onCheckForUpdates()
                }
            },
            icon = Icons.Default.SystemUpdate,
            isLoading = state.isCheckingForUpdates ||
                    state.isDownloadingUpdate ||
                    state.isInstallingUpdate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LoggingSettingsSection(
    state: SettingsState,
    onLogLevelChange: (LogLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.logging_settings),
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.log_level_selection),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CarouselValueButton(
            values = LogLevel.values().toList(),
            currentValue = state.logLevel,
            onValueChange = onLogLevelChange,
            valueToString = { logLevel ->
                when(logLevel) {
                    LogLevel.FULL -> stringResource(id = R.string.log_level_full)
                    LogLevel.INFO -> stringResource(id = R.string.log_level_info)
                    LogLevel.WARNING -> stringResource(id = R.string.log_level_warning)
                    LogLevel.ERROR -> stringResource(id = R.string.log_level_error)
                }
            },
            // Можно добавить иконку, например Icons.Filled.ChangeCircle
            modifier = Modifier.fillMaxWidth()
        )

        // Пояснение для каждого уровня
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when(state.logLevel) {
                LogLevel.FULL -> stringResource(id = R.string.log_level_full_description)
                LogLevel.INFO -> stringResource(id = R.string.log_level_info_description)
                LogLevel.WARNING -> stringResource(id = R.string.log_level_warning_description)
                LogLevel.ERROR -> stringResource(id = R.string.log_level_error_description)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}