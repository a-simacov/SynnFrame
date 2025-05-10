package com.synngate.synnframe.presentation.ui.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import java.time.format.DateTimeFormatter

@Composable
fun SyncHistoryScreen(
    viewModel: SyncHistoryViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    AppScaffold(
        title = stringResource(id = R.string.sync_history_title),
        onNavigateBack = navigateBack,
        isLoading = state.isLoading,
        notification = state.error?.let { Pair(it, StatusType.ERROR) }
    ) { paddingValues ->
        if (state.syncHistory.isEmpty() && !state.isLoading) {
            // Показываем пустое состояние
            EmptyHistoryView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Показываем историю синхронизаций
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Статистика синхронизаций
                SyncStatisticsCard(
                    successCount = state.syncHistory.count { it.successful },
                    failureCount = state.syncHistory.count { !it.successful },
                    totalItemsProcessed = state.syncHistory.sumOf { it.totalOperations },
                    averageDuration = if (state.syncHistory.isNotEmpty())
                        state.syncHistory.map { it.duration }.average().toLong() else 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Список записей истории
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.syncHistory) { record ->
                        SyncHistoryItem(
                            record = record,
                            onClick = { viewModel.onHistoryItemClick(record) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Диалог с деталями синхронизации
    if (state.selectedRecord != null) {
        SyncDetailsDialog(
            record = state.selectedRecord!!,
            onDismiss = { viewModel.closeDetails() }
        )
    }
}

@Composable
fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.no_sync_history),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.sync_history_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SyncStatisticsCard(
    successCount: Int,
    failureCount: Int,
    totalItemsProcessed: Int,
    averageDuration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.sync_statistics),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = stringResource(id = R.string.successful_syncs),
                    value = successCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    label = stringResource(id = R.string.failed_syncs),
                    value = failureCount.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = stringResource(id = R.string.items_processed),
                    value = totalItemsProcessed.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                StatItem(
                    label = stringResource(id = R.string.average_duration),
                    value = formatDuration(averageDuration),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SyncHistoryItem(
    record: SyncHistoryRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка статуса
            Icon(
                imageVector = if (record.successful) Icons.Default.Done else Icons.Default.Error,
                contentDescription = null,
                tint = if (record.successful)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о синхронизации
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.startTime.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(
                        id = R.string.sync_item_summary,
                        record.productsDownloaded
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!record.successful && record.errorMessage != null) {
                    Text(
                        text = record.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Длительность
            Text(
                text = formatDuration(record.duration),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SyncDetailsDialog(
    record: SyncHistoryRecord,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.sync_details_title))
        },
        text = {
            Column {
                DetailItem(
                    label = stringResource(id = R.string.sync_start_time),
                    value = record.startTime.format(dateFormatter)
                )

                DetailItem(
                    label = stringResource(id = R.string.sync_end_time),
                    value = record.endTime.format(dateFormatter)
                )

                DetailItem(
                    label = stringResource(id = R.string.sync_duration),
                    value = formatDuration(record.duration)
                )

                DetailItem(
                    label = stringResource(id = R.string.sync_network_type),
                    value = record.networkType
                )

                DetailItem(
                    label = stringResource(id = R.string.sync_metered_connection),
                    value = if (record.meteredConnection)
                        stringResource(id = R.string.yes)
                    else
                        stringResource(id = R.string.no)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailItem(
                    label = stringResource(id = R.string.sync_products_downloaded),
                    value = record.productsDownloaded.toString()
                )

                DetailItem(
                    label = stringResource(id = R.string.sync_retry_attempts),
                    value = record.retryAttempts.toString()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailItem(
                    label = stringResource(id = R.string.sync_status),
                    value = if (record.successful)
                        stringResource(id = R.string.sync_success)
                    else
                        stringResource(id = R.string.sync_failure),
                    valueColor = if (record.successful)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                if (!record.successful && record.errorMessage != null) {
                    DetailItem(
                        label = stringResource(id = R.string.sync_error),
                        value = record.errorMessage,
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

// Вспомогательная функция для форматирования длительности
fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    if (seconds < 60) {
        return "$seconds сек"
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes мин $remainingSeconds сек"
}