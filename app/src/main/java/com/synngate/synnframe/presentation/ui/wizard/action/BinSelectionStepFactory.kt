package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.taskx.components.BinItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel

/**
 * Фабрика компонентов для шага выбора ячейки
 */
class BinSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }

        // Получаем зону из контекста, если указана
        val zoneFilter = action.placementBin?.zone

        // Получение данных из ViewModel
        val bins by wizardViewModel.bins.collectAsState()

        // Запланированная ячейка (может быть null)
        val plannedBin = action.placementBin

        // Список запланированных ячеек
        val planBins = remember(action) {
            listOfNotNull(plannedBin)
        }

        // Получаем уже выбранную ячейку из контекста, если есть
        val selectedBin = remember(context.results) {
            context.results[step.id] as? BinX
        }

        // Загрузка ячеек при изменении поискового запроса
        LaunchedEffect(zoneFilter, searchQuery) {
            wizardViewModel.loadBins(searchQuery, zoneFilter)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображаем выбранную ячейку, если есть
            if (selectedBin != null) {
                Text(
                    text = "Выбранная ячейка:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Код: ${selectedBin.code}",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Зона: ${selectedBin.zone}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Расположение: ${selectedBin.line}-${selectedBin.rack}-${selectedBin.tier}-${selectedBin.position}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Кнопка "Вперёд" для перехода к следующему шагу
                        if (context.hasStepResult) {
                            androidx.compose.material3.Button(
                                onClick = { context.onForward() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Вперёд")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные ячейки, если они есть
            if (planBins.isNotEmpty()) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                ) {
                    items(planBins) { bin ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Код: ${bin.code}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Зона: ${bin.zone}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                androidx.compose.material3.Button(
                                    onClick = {
                                        // Явно указываем тип Any
                                        val result: Any = bin
                                        context.onComplete(result)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск ячейки") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(bins) { bin ->
                    BinItem(
                        bin = bin,
                        onClick = {
                            // Явно указываем тип Any
                            val result: Any = bin
                            context.onComplete(result)
                        }
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is BinX
    }
}