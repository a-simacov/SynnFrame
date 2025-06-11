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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.login.model.LoginEvent

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    navigateToMainMenu: () -> Unit,
    navigateToServersList: () -> Unit,
    exitApp: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCameraScannerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToMainMenu -> navigateToMainMenu()
                is LoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is LoginEvent.ShowExitConfirmation -> {
                    // Обрабатывается через showExitConfirmation в state
                }
                is LoginEvent.ExitApp -> exitApp()
            }
        }
    }

    BackHandler {
        viewModel.showExitConfirmation()
    }

    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.updatePassword(barcode)
            viewModel.login()
        }
    )

    if (showCameraScannerDialog) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.updatePassword(barcode)
                viewModel.login()
                showCameraScannerDialog = false
            },
            onClose = {
                showCameraScannerDialog = false
            },
            instructionText = "Scan password barcode",
            allowManualInput = true
        )
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
        title = stringResource(id = R.string.login_title),
        actions = {
            IconButton(onClick = { showCameraScannerDialog = true }) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = stringResource(R.string.scan_with_camera)
                )
            }
        },
        snackbarHostState = snackbarHostState,
        useScanner = true
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.Center,
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
                PasswordTextField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    onImeAction = { viewModel.login() },
                    label = stringResource(id = R.string.password),
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                ActionButton(
                    text = stringResource(id = R.string.login),
                    onClick = { viewModel.login() },
                    isLoading = state.isLoading,
                    enabled = !state.isLoading && state.password.isNotEmpty() && state.hasActiveServer,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationButton(
                    text = stringResource(id = R.string.exit),
                    onClick = { viewModel.showExitConfirmation() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationButton(
                    text = stringResource(id = R.string.servers_title),
                    onClick = navigateToServersList,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

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