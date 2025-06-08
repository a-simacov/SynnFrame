package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun SummaryScreen(
    state: ActionWizardState,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Итог действия",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        state.factAction?.let { factAction ->
            factAction.storageProductClassifier?.let { product ->
                SummaryItem(
                    title = "Товар из классификатора",
                    value = "${product.name} (${product.id})"
                )
            }

            factAction.storageProduct?.let { taskProduct ->
                SummaryItem(
                    title = "Товар",
                    value = "${taskProduct.product.name} (${taskProduct.product.id})"
                )

                if (taskProduct.hasExpirationDate()) {
                    SummaryItem(
                        title = "Срок годности",
                        value = taskProduct.expirationDate.toString()
                    )
                }

                SummaryItem(
                    title = "Статус товара",
                    value = taskProduct.status.format()
                )
            }

            factAction.storageBin?.let { bin ->
                SummaryItem(
                    title = "Ячейка хранения",
                    value = bin.code
                )
            }

            factAction.storagePallet?.let { pallet ->
                SummaryItem(
                    title = "Паллета хранения",
                    value = pallet.code
                )
            }

            factAction.placementBin?.let { bin ->
                SummaryItem(
                    title = "Ячейка размещения",
                    value = bin.code
                )
            }

            factAction.placementPallet?.let { pallet ->
                SummaryItem(
                    title = "Паллета размещения",
                    value = pallet.code
                )
            }

            if (factAction.quantity > 0) {
                SummaryItem(
                    title = "Количество",
                    value = factAction.quantity.toString()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.sendingFailed) {
            ErrorPanel(
                message = state.error ?: "Не удалось отправить данные на сервер",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        if (state.sendingFailed) {
            // Дополнительная кнопка выхода при ошибке
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Выйти без сохранения")
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Повторить отправку")
                }
            }
        } else {
            // Обычные кнопки при нормальной работе
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Text("Назад")
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Завершить")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorPanel(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )

        Divider(modifier = Modifier.padding(top = 4.dp))
    }
}