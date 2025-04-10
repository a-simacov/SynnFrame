package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.presentation.ui.taskx.components.SummaryStep

@Composable
fun FactLineWizard(
    viewModel: TaskXDetailViewModel,
    modifier: Modifier = Modifier
) {
    // Получаем состояние мастера из ViewModel
    val wizardState by viewModel.factLineWizardController.wizardState.collectAsState()

    if (wizardState == null) return

    Dialog(onDismissRequest = { viewModel.cancelWizard() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier.fillMaxSize(0.95f)
        ) {
            FactLineWizardContent(
                state = wizardState!!,
                onCancel = { viewModel.cancelWizard() },
                onStepComplete = { result -> viewModel.processWizardStep(result) },
                onWizardComplete = { viewModel.completeWizard() }
            )
        }
    }
}

@Composable
private fun FactLineWizardContent(
    state: FactLineWizardState,
    onCancel: () -> Unit,
    onStepComplete: (Any?) -> Unit,
    onWizardComplete: () -> Unit,
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
                    onStepComplete = onStepComplete
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