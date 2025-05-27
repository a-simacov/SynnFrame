package com.synngate.synnframe.presentation.ui.taskx.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun SummaryScreen(
    state: ActionWizardState,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
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

        // Отображаем информацию о действии
        state.factAction?.let { factAction ->
            // Товар из классификатора
            factAction.storageProductClassifier?.let { product ->
                SummaryItem(
                    title = "Товар из классификатора",
                    value = "${product.name} (${product.id})"
                )
            }

            // Товар задания
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

            // Ячейка хранения
            factAction.storageBin?.let { bin ->
                SummaryItem(
                    title = "Ячейка хранения",
                    value = bin.code
                )
            }

            // Паллета хранения
            factAction.storagePallet?.let { pallet ->
                SummaryItem(
                    title = "Паллета хранения",
                    value = pallet.code
                )
            }

            // Ячейка размещения
            factAction.placementBin?.let { bin ->
                SummaryItem(
                    title = "Ячейка размещения",
                    value = bin.code
                )
            }

            // Паллета размещения
            factAction.placementPallet?.let { pallet ->
                SummaryItem(
                    title = "Паллета размещения",
                    value = pallet.code
                )
            }

            // Количество
            if (factAction.quantity > 0) {
                SummaryItem(
                    title = "Количество",
                    value = factAction.quantity.toString()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Если была ошибка отправки, показываем сообщение
        if (state.sendingFailed) {
            Text(
                text = "Не удалось отправить данные на сервер. Вы можете повторить попытку или выйти без сохранения.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Кнопки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Выйти без сохранения")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Назад")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Завершить")
            }
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