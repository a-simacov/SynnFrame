package com.synngate.synnframe.presentation.ui.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.buttons.BooleanButton
import com.synngate.synnframe.presentation.common.buttons.CarouselValueButton
import com.synngate.synnframe.presentation.common.dialog.ProgressDialog
import com.synngate.synnframe.presentation.common.dialog.QrCodeDialog
import com.synngate.synnframe.presentation.common.inputs.AppTextField
import com.synngate.synnframe.presentation.common.inputs.NumberTextField
import com.synngate.synnframe.presentation.common.inputs.PasswordTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.InfoCard
import com.synngate.synnframe.presentation.common.scaffold.ScrollableScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailEvent
import com.synngate.synnframe.presentation.ui.server.model.ServerDetailState
import kotlinx.coroutines.launch

/**
 * Экран деталей сервера с использованием ViewModel
 */
@Composable
fun ServerDetailScreen(
    viewModel: ServerDetailViewModel,
    navigateBack: () -> Unit
) {
    // Состояние экрана из ViewModel
    val state by viewModel.uiState.collectAsState()

    // Эффект для обработки событий
    val events = viewModel.events
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Состояние для отображения QR-кода
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var qrCodeContent by remember { mutableStateOf("") }

    // Состояние для отображения диалога сканирования
    var showScannerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = viewModel) {
        events.collect { event ->
            when (event) {
                is ServerDetailEvent.NavigateBack -> navigateBack()
                is ServerDetailEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }

                is ServerDetailEvent.ConnectionSuccess -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.server_connection_success),
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                is ServerDetailEvent.ConnectionError -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.server_connection_error,
                                event.message
                            ),
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                is ServerDetailEvent.ServerSaved -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.server_saved_success)
                        )
                    }
                }

                is ServerDetailEvent.ShowQrCode -> {
                    qrCodeContent = event.content
                    showQrCodeDialog = true
                }
            }
        }
    }

    // Диалог прогресса при загрузке
    if (state.isLoading || state.isTestingConnection || state.isSaving) {
        val message = when {
            state.isTestingConnection -> stringResource(id = R.string.testing_connection)
            state.isSaving -> stringResource(id = R.string.saving)
            else -> stringResource(id = R.string.loading)
        }
        ProgressDialog(message = message)
    }

    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.updatePassword(barcode)
            viewModel.handleQrCodeScan(barcode)
        }
    )

    // Диалог отображения QR-кода
    if (showQrCodeDialog) {
        QrCodeDialog(
            content = qrCodeContent,
            onDismiss = { showQrCodeDialog = false }
        )
    }

    // Диалог сканирования QR-кода
    if (showScannerDialog) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.handleQrCodeScan(barcode)
                showScannerDialog = false
            },
            onClose = { showScannerDialog = false },
            instructionText = stringResource(id = R.string.scan_qr_code_instruction),
            title = stringResource(id = R.string.scan_qr_code)
        )
    }

    // Основной экран
    AppScaffold(
        title = stringResource(
            id = if (state.isEditMode)
                R.string.server_edit_title
            else
                R.string.server_add_title
        ),
        onNavigateBack = { viewModel.navigateBack() },
        snackbarHostState = snackbarHostState,
        useScanner = true
    ) { paddingValues ->
        ServerDetailContent(
            state = state,
            onNameChange = viewModel::updateName,
            onHostChange = viewModel::updateHost,
            onPortChange = viewModel::updatePort,
            onApiEndpointChange = viewModel::updateApiEndpoint,
            onLoginChange = viewModel::updateLogin,
            onPasswordChange = viewModel::updatePassword,
            onActiveToggle = viewModel::updateIsActive,
            onTestConnection = viewModel::testConnection,
            onSave = viewModel::saveServer,
            onBack = viewModel::navigateBack,
            onScanQrCode = { showScannerDialog = true },
            onGenerateQrCode = viewModel::generateQrCode,
            onUpdateScannerType = viewModel::updateScannerType,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun ServerDetailContent(
    state: ServerDetailState,
    onNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onApiEndpointChange: (String) -> Unit,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onActiveToggle: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onScanQrCode: () -> Unit,
    onGenerateQrCode: () -> Unit,
    onUpdateScannerType: (DeviceType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ScrollableScreenContent(
        modifier = modifier
    ) {
        // QR-код действия
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Кнопка сканирования QR-кода
            ActionButton(
                text = "Read",
                onClick = onScanQrCode,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.QrCodeScanner,
                buttonHeight = 36f,
                textStyle = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка генерации QR-кода
            ActionButton(
                text = "Show",
                onClick = onGenerateQrCode,
                icon = Icons.Default.QrCode2,
                modifier = Modifier.weight(1f),
                buttonHeight = 36f,
                textStyle = MaterialTheme.typography.titleSmall,
                enabled = state.name.isNotBlank() && state.host.isNotBlank() &&
                        state.port.isNotBlank() && state.apiEndpoint.isNotBlank() &&
                        state.login.isNotBlank() && state.password.isNotBlank()
            )
        }

        // Карточка с основными данными сервера
        InfoCard(
            title = stringResource(id = R.string.server_connection_settings)
        ) {
            // Поле имени сервера
            AppTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(id = R.string.server_name),
                isError = state.validationError?.contains("name", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("name", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле хоста
            AppTextField(
                value = state.host,
                onValueChange = onHostChange,
                label = stringResource(id = R.string.server_host),
                isError = state.validationError?.contains("host", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("host", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле порта
            NumberTextField(
                value = state.port,
                onValueChange = onPortChange,
                label = stringResource(id = R.string.server_port),
                isError = state.validationError?.contains("port", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("port", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле точки подключения API
            AppTextField(
                value = state.apiEndpoint,
                onValueChange = onApiEndpointChange,
                label = stringResource(id = R.string.server_api_endpoint),
                isError = state.validationError?.contains("enpoint", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("enpoint", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Карточка с учетными данными
        InfoCard(
            title = stringResource(id = R.string.server_credentials)
        ) {
            // Поле логина
            AppTextField(
                value = state.login,
                onValueChange = onLoginChange,
                label = stringResource(id = R.string.server_login),
                isError = state.validationError?.contains("login", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("login", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Поле пароля
            PasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(id = R.string.server_password),
                isError = state.validationError?.contains("password", ignoreCase = true) == true,
                errorText = state.validationError?.takeIf {
                    it.contains("password", ignoreCase = true)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Проверка подключения
        ActionButton(
            text = stringResource(id = R.string.server_test_connection),
            onClick = onTestConnection,
            enabled = !state.isTestingConnection,
            isLoading = state.isTestingConnection,
            icon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Статус подключения
        Text(
            text = state.connectionStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BooleanButton(
            currentValue = state.isActive,
            onValueChange = onActiveToggle,
            modifier = Modifier.fillMaxWidth(),
            valueToString = {
                stringResource(
                    id = if (it) R.string.server_active else R.string.server_inactive
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.showScannerTypeOptions) {
            val types = DeviceType.entries
            CarouselValueButton(
                values = types,
                currentValue = state.currentScannerType,
                onValueChange = { onUpdateScannerType(it) },
                valueToString = { deviceType ->
                    when (deviceType) {
                        DeviceType.STANDARD -> "Standard (no auto-scan)"
                        DeviceType.ZEBRA_DATAWEDGE -> stringResource(R.string.scanner_type_datawedge)
                        DeviceType.CAMERA_SCANNER -> "Camera"
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ActionButton(
            text = stringResource(id = R.string.save),
            onClick = onSave,
            enabled = !state.isSaving,
            isLoading = state.isSaving,
            icon = Icons.Default.Save,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}