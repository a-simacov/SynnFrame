package com.synngate.synnframe.presentation.ui.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.domain.model.wizard.WizardState
import timber.log.Timber

@Composable
fun WizardScreen(
    state: WizardState?,
    onStepComplete: (Any?) -> Unit,
    onStepSkip: (Any?) -> Unit,  // Новый параметр
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Проверяем состояние и его инициализацию
    if (state == null || !state.isInitialized) {
        Timber.w("WizardScreen: state is null or not initialized")
        return
    }

    Dialog(
        onDismissRequest = {
            // Добавляем логирование при закрытии
            Timber.d("Dialog was closed by user")
            onCancel()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Добавление строки факта",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = {
                        Timber.d("CLose wizard button was clicked")
                        onCancel()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                // Прогресс
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                // Содержимое текущего шага (в безопасном блоке)
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                ) {
                    val currentStep = state.currentStep

                    if (currentStep == null || state.isCompleted) {
                        // Итоговый экран
                        SummaryScreen(
                            results = state.results,
                            onComplete = {
                                Timber.d("Wizard complete button was clicked")
                                onComplete()
                            },
                            onCancel = {
                                Timber.d("Cancel button was pressed in summary screen")
                                onCancel()
                            }
                        )
                    } else {
                        // Текущий шаг
                        val context = WizardContext(
                            results = state.results,
                            onUpdate = { updatedResults -> onStepComplete(updatedResults) },
                            onBack = { onStepComplete(null) },
                            onSkip = { result -> onStepSkip(result) },
                            onCancel = onCancel
                        )

                        currentStep.content(context)
                    }
                }

                // Навигационные кнопки
                if (state.currentStep != null && !state.isCompleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Timber.d("Cancel button was pressed in navigation")
                                onCancel()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }

                        if (state.canGoBack) {
                            OutlinedButton(
                                onClick = {
                                    Timber.d("Back button was pressed in navigation")
                                    onStepComplete(null)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Назад")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.05f).height(8.dp))
            }
        }
    }
}

@Composable
fun SummaryScreen(
    results: WizardResultModel,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Отображение продукта
        val product = results.storageProduct
        if (product != null) {
            Text(
                text = "Товар: ${product.product.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Артикул: ${product.product.articleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Количество: ${product.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (product.hasExpirationDate()) {
                Text(
                    text = "Срок годности: ${product.expirationDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = "Статус: ${product.status}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображение паллеты хранения
        val storagePallet = results.storagePallet
        if (storagePallet != null) {
            Text(
                text = "Паллета хранения: ${storagePallet.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Статус: ${if (storagePallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображение паллеты размещения
        val placementPallet = results.placementPallet
        if (placementPallet != null && placementPallet != storagePallet) {
            Text(
                text = "Паллета размещения: ${placementPallet.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Статус: ${if (placementPallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображение ячейки размещения
        val placementBin = results.placementBin
        if (placementBin != null) {
            Text(
                text = "Ячейка размещения: ${placementBin.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Зона: ${placementBin.zone}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображение действия WMS
        val wmsAction = results.wmsAction
        if (wmsAction != null) {
            Text(
                text = "Действие WMS: ${wmsActionToString(wmsAction)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Подтвердить")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

// Вспомогательная функция для преобразования WmsAction в строку
@Composable
private fun wmsActionToString(action: WmsAction): String {
    return when (action) {
        WmsAction.PUT_INTO -> "Положить"
        WmsAction.TAKE_FROM -> "Взять"
        WmsAction.RECEIPT -> "Оприходовать"
        WmsAction.EXPENSE -> "Списать"
        WmsAction.RECOUNT -> "Пересчитать"
        WmsAction.USE -> "Использовать"
    }
}