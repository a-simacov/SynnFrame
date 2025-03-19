package com.synngate.synnframe.presentation.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.buttons.PropertyToggleButton
import com.synngate.synnframe.presentation.common.buttons.SelectableButton
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
import timber.log.Timber

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navigateToServerList: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Состояние экрана из ViewModel
    val state by viewModel.uiState.collectAsState()

    // SnackbarHostState для показа уведомлений
    val snackbarHostState = rememberSaveable { SnackbarHostState() }

    // Контекст для работы с файловой системой и установкой обновлений
    val context = LocalContext.current

    // Обработка событий из ViewModel
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
            }
        }
    }

    // Оптимизация: используем derivedStateOf для проверки условий вместо прямых вычислений
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

    // Отображение диалога обновления если доступно
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

    // Диалог прогресса при загрузке - используем переменную isLoading
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
            // Активный внешний сервер
            ActiveServerSection(
                state = state,
                onNavigateToServerList = navigateToServerList,
                onShowServersOnStartupChange = viewModel::updateShowServersOnStartup,
                onPeriodicUploadEnabledChange = viewModel::updatePeriodicUpload,
                onUploadIntervalChange = viewModel::updateUploadInterval
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Локальный веб-сервер
            WebServerSection(
                state = state,
                onToggleWebServer = viewModel::toggleWebServer
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

            // Обновление
            UpdateSection(
                state = state,
                onCheckForUpdates = viewModel::checkForUpdates,
                onShowUpdateConfirmDialog = viewModel::showUpdateConfirmDialog,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Обработка ошибок
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
        // Кнопка перехода к списку серверов
        NavigationButton(
            text = stringResource(id = R.string.navigate_to_servers),
            onClick = onNavigateToServerList,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Информация об активном сервере
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

        Divider()

        // Настройка отображения при запуске
        Spacer(modifier = Modifier.height(16.dp))
        PropertyToggleButton(
            property = stringResource(id = R.string.server_show_on_startup),
            value = state.showServersOnStartup,
            onToggle = onShowServersOnStartupChange,
            modifier = Modifier.fillMaxWidth()
        )

        // Настройка периодической выгрузки
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
                    // Поле ввода интервала
                    NumberTextField(
                        value = state.uploadIntervalSeconds.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { onUploadIntervalChange(it) }
                        },
                        label = stringResource(id = R.string.interval_seconds),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Информационный текст
                    Text(
                        text = stringResource(id = R.string.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                // Подсказка по диапазону
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
        // Выбор темы
        Text(
            text = stringResource(id = R.string.theme_selection),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Добавляем key для каждой кнопки
            ThemeMode.values().forEach { theme ->
                key(theme) {
                    ThemeModeButton(
                        text = when(theme) {
                            ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                            ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                        },
                        selected = state.themeMode == theme,
                        onClick = { onThemeModeChange(theme) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Выбор языка
        Text(
            text = stringResource(id = R.string.language_selection),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Кнопки выбора языка
            LanguageButton(
                text = "Русский",
                selected = state.languageCode == "ru",
                onClick = { onLanguageCodeChange("ru") }
            )

            LanguageButton(
                text = "English",
                selected = state.languageCode == "en",
                onClick = { onLanguageCodeChange("en") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
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
                modifier = Modifier.fillMaxWidth()
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
private fun ThemeModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableButton(
        text = text,
        isSelected = selected,
        onClick = onClick,
        modifier = modifier,
        buttonHeight = 48f // Уменьшенная высота для вариантов выбора
    )
}

@Composable
private fun LanguageButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableButton(
        text = text,
        isSelected = selected,
        onClick = onClick,
        modifier = modifier,
        buttonHeight = 48f // Уменьшенная высота для вариантов выбора
    )
}

@Composable
fun UpdateSection(
    state: SettingsState,
    onCheckForUpdates: () -> Unit,
    onShowUpdateConfirmDialog: () -> Unit, // Добавляем функцию для показа диалога
    modifier: Modifier = Modifier
) {
    InfoCard(
        title = stringResource(id = R.string.update_settings),
        modifier = modifier
    ) {
        // Анимированное появление информации о доступной версии
        AnimatedVisibility(
            visible = state.lastVersion != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Text(
                    text = stringResource(
                        id = R.string.available_version,
                        state.lastVersion ?: ""
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
        }

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
            isLoading = state.isCheckingForUpdates ||
                    state.isDownloadingUpdate ||
                    state.isInstallingUpdate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}