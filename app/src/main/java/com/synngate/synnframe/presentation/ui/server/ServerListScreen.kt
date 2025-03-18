package com.synngate.synnframe.presentation.ui.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.di.AppContainer
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Временная заглушка для экрана списка серверов.
 * Будет заменена на полноценную реализацию в следующих этапах.
 */
@Composable
fun ServerListScreen(
    appContainer: AppContainer,
    navigateToLogin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Получаем значение настройки "показывать при запуске"
    val showOnStartup by appContainer.appSettingsDataStore.showServersOnStartup
        .collectAsState(initial = true)

    // Состояние для отображения списка серверов
    var serversExist by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.servers_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!serversExist) {
                // Если список серверов пуст
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.servers_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Временная кнопка для симуляции добавления сервера
                    Button(
                        onClick = {
                            Timber.d("Add server button clicked")
                            serversExist = true
                        }
                    ) {
                        Text(stringResource(id = R.string.add))
                    }
                }
            } else {
                // Если есть серверы (временная заглушка)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Server 1 (localhost:8080)",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Временная кнопка для симуляции добавления сервера
                    Button(
                        onClick = {
                            Timber.d("Add another server button clicked")
                        }
                    ) {
                        Text(stringResource(id = R.string.add))
                    }
                }
            }

            // Переключатель для настройки "показывать при запуске"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Switch(
                        checked = showOnStartup,
                        onCheckedChange = {
                            coroutineScope.launch {
                                Timber.d("Show on startup setting changed: $it")
                                appContainer.appSettingsDataStore.setShowServersOnStartup(it)
                            }
                        }
                    )

                    Text(
                        text = stringResource(id = R.string.server_show_on_startup),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        Timber.d("Continue button clicked")
                        navigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.continue_button))
                }
            }
        }
    }
}