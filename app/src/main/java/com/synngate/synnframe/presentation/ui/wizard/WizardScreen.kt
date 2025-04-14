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
    onStepSkip: (Any?) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Проверяем состояние и его инициализацию
    if (state == null || !state.isInitialized) {
        Timber.w("WizardScreen: state is null or not initialized")
        return
    }

    // Обработчик действий при нажатии на "Назад"
    val handleBack = {
        Timber.d("WizardScreen: handleBack called, isCompleted=${state.isCompleted}, canGoBack=${state.canGoBack}")

        if (state.isCompleted) {
            // На итоговом экране
            Timber.d("handleBack: returning from final screen")
            onStepComplete(null)
        } else if (state.canGoBack) {
            // На промежуточном шаге с возможностью возврата
            Timber.d("handleBack: going to previous step")
            onStepComplete(null)
        } else {
            // На первом шаге - закрываем визард
            Timber.d("handleBack: cancelling wizard")
            onCancel()
        }
    }

    Dialog(
        // При нажатии системной кнопки "Назад" вызовется onDismissRequest
        onDismissRequest = {
            Timber.d("Dialog onDismissRequest triggered - handling system back button")
            handleBack()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true, // ВАЖНО: включаем стандартную обработку
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
                        Timber.d("Close wizard button was clicked")
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

                // Содержимое текущего шага
                Box(
                    modifier = Modifier
                        .weight(1f)
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
                            onBack = {
                                // Используем общий обработчик
                                Timber.d("Back button was pressed in summary screen")
                                handleBack()
                            }
                        )
                    } else {
                        // Текущий шаг
                        val context = WizardContext(
                            results = state.results,
                            onUpdate = { updatedResults -> onStepComplete(updatedResults) },
                            onBack = {
                                Timber.d("Step requested navigation back")
                                handleBack()
                            },
                            onSkip = { result -> onStepSkip(result) },
                            onCancel = onCancel
                        )

                        currentStep.content(context)
                    }
                }

                // Навигационные кнопки - только кнопка назад, если это возможно
                if (state.currentStep != null && !state.isCompleted && state.canGoBack) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        OutlinedButton(
                            onClick = {
                                Timber.d("Back button was pressed in navigation")
                                handleBack()
                            }
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

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SummaryScreen(
    results: WizardResultModel,
    onComplete: () -> Unit,
    onBack: () -> Unit,
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
                onClick = {
                    Timber.d("Back button in summary screen clicked")
                    onBack()
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