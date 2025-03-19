package com.synngate.synnframe.presentation.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.buttons.NavigationButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.inputs.PasswordTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.ui.login.model.LoginEvent

/**
 * Экран логина
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    navigateToMainMenu: () -> Unit,
    navigateToServersList: () -> Unit,
    exitApp: () -> Unit
) {
    // Получаем состояние экрана из ViewModel
    val state by viewModel.uiState.collectAsState()

    // SnackbarHostState для показа уведомлений
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    // Обработка событий из ViewModel
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToMainMenu -> navigateToMainMenu()
                is LoginEvent.NavigateToServerList -> navigateToServersList()
                is LoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is LoginEvent.ShowExitConfirmation -> {
                    // Обрабатывается через showExitConfirmation в state
                }
                is LoginEvent.ExitApp -> exitApp()
            }
        }
    }

    // Обработка нажатия кнопки Назад
    BackHandler {
        viewModel.showExitConfirmation()
    }

    // Диалог подтверждения выхода
    if (state.showExitConfirmation) {
        ConfirmationDialog(
            title = stringResource(id = R.string.exit_confirmation_title),
            message = stringResource(id = R.string.exit_confirmation_message),
            onConfirm = { viewModel.exitApp() },
            onDismiss = { viewModel.hideExitConfirmation() }
        )
    }

    // Основной UI экрана
    AppScaffold(
        title = stringResource(id = R.string.login_title),
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Отображаем информацию об активном сервере
                if (!state.hasActiveServer) {
                    Text(
                        text = stringResource(id = R.string.no_active_server),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    NavigationButton(
                        text = stringResource(id = R.string.navigate_to_servers),
                        onClick = { viewModel.navigateToServerList() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Поле ввода пароля
                PasswordTextField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = stringResource(id = R.string.password),
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка логина
                ActionButton(
                    text = stringResource(id = R.string.login),
                    onClick = { viewModel.login() },
                    isLoading = state.isLoading,
                    enabled = !state.isLoading && state.password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка выхода
                NavigationButton(
                    text = stringResource(id = R.string.exit),
                    onClick = { viewModel.showExitConfirmation() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Информация о версии приложения
                Text(
                    text = stringResource(
                        id = R.string.splash_version,
                        BuildConfig.VERSION_NAME
                    ),
                    textAlign = TextAlign.Center,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}