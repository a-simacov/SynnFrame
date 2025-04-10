package com.synngate.synnframe.presentation.ui.taskx

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
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.taskx.components.SummaryStep
import com.synngate.synnframe.presentation.ui.taskx.wizard.WizardContext
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

@Composable
fun FactLineWizard(
    viewModel: TaskXDetailViewModel,
    modifier: Modifier = Modifier
) {
    // Получаем состояние мастера из контроллера
    val wizardState by viewModel.factLineWizardController.wizardState.collectAsState()

    if (wizardState == null) return

    Dialog(onDismissRequest = { viewModel.cancelWizard() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier.fillMaxSize(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок мастера
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Добавление строки факта",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { viewModel.cancelWizard() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                }

                // Прогресс
                LinearProgressIndicator(
                    progress = { wizardState!!.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                // Текущий шаг
                val currentStep = wizardState!!.currentStep

                if (currentStep == null || wizardState!!.isCompleted) {
                    // Показываем итоговый экран с результатами
                    SummaryStep(
                        results = wizardState!!.results,
                        onComplete = { viewModel.completeWizard() },
                        onCancel = { viewModel.cancelWizard() }
                    )
                } else {
                    // Показываем текущий шаг
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val context = WizardContext(
                            results = wizardState!!.results,
                            onComplete = { result ->
                                viewModel.processWizardStep(result)
                            },
                            onBack = {
                                viewModel.processWizardStep(null)
                            }
                        )

                        // Рендерим содержимое текущего шага
                        currentStep.content(context)
                    }

                    // Кнопки навигации
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelWizard() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }

                        if (wizardState!!.canGoBack) {
                            OutlinedButton(
                                onClick = { viewModel.processWizardStep(null) },
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
            }
        }
    }
}

@Composable
fun SummaryStep(
    results: Map<String, Any?>,
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
        val product = results.values.filterIsInstance<TaskProduct>().firstOrNull()
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

        // Отображение паллеты размещения
        val pallet = results.values.filterIsInstance<Pallet>().firstOrNull()
        if (pallet != null) {
            Text(
                text = "Паллета: ${pallet.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Отображение ячейки размещения
        val bin = results.values.filterIsInstance<BinX>().firstOrNull()
        if (bin != null) {
            Text(
                text = "Ячейка: ${bin.code}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки действий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
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

@Composable
private fun FactLineWizardContent(
    state: FactLineWizardState,
    onCancel: () -> Unit,
    onStepComplete: (Any?) -> Unit,
    onWizardComplete: () -> Unit,
    wizardViewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок мастера
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Добавление строки факта",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть"
                )
            }
        }

        // Прогресс
        LinearProgressIndicator(
            progress = { state.getProgressValue() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        // Текущее действие
        val currentAction = state.getCurrentAction()

        if (currentAction == null || state.isCompleted()) {
            // Завершающий шаг
            SummaryStep(
                state = state,
                onComplete = onWizardComplete,
                onCancel = onCancel
            )
        } else {
            // Текущее действие
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Используем фабрику для создания нужного типа шага
                FactLineWizardStepFactory.CreateStep(
                    action = currentAction,
                    intermediateResults = state.getIntermediateResults(),
                    onStepComplete = onStepComplete,
                    wizardViewModel = wizardViewModel // Передаем ViewModel визарда
                )
            }

            // Кнопки навигации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }

                val canGoBack = state.canGoBack()

                if (canGoBack) {
                    OutlinedButton(
                        onClick = { onStepComplete(null) }, // null для возврата назад
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

                // Кнопка "Далее" должна иметь обработчик из конкретного шага
                Button(
                    onClick = { /* Будет заполнено в шагах */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (state.isLastStep()) "Завершить" else "Далее"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (state.isLastStep())
                            Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}