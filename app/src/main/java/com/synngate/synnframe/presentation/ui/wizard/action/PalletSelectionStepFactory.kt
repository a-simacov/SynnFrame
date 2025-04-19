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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.taskx.components.PalletItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel

/**
 * Фабрика компонентов для шага выбора паллеты
 */
class PalletSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }

        // Получение данных из ViewModel
        val pallets by wizardViewModel.pallets.collectAsState()

        // Определяем, ищем мы паллету хранения или размещения
        // Это зависит от того, к какому типу шагов относится текущий шаг
        val isStorageStep = step.id in action.actionTemplate.storageSteps.map { it.id }

        // Запланированная паллета (может быть null)
        val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet

        // Список запланированных паллет
        val planPallets = remember(action, isStorageStep) {
            listOfNotNull(plannedPallet)
        }

        // Получаем уже выбранную паллету из контекста, если есть
        val selectedPallet = remember(context.results) {
            context.results[step.id] as? Pallet
        }

        // Загрузка паллет при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            wizardViewModel.loadPallets(searchQuery)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображаем выбранную паллету, если есть
            if (selectedPallet != null) {
                Text(
                    text = "Выбранная паллета:",
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
                            text = "Код: ${selectedPallet.code}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Статус: ${if (selectedPallet.isClosed) "Закрыта" else "Открыта"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные паллеты, если они есть
            if (planPallets.isNotEmpty()) {
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
                    items(planPallets) { pallet ->
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
                                    text = "Код: ${pallet.code}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                androidx.compose.material3.Button(
                                    onClick = {
                                        // Явно указываем тип Any
                                        val result: Any = pallet
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
                label = { Text("Поиск паллеты") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(pallets) { pallet ->
                    PalletItem(
                        pallet = pallet,
                        onClick = {
                            // Явно указываем тип Any
                            val result: Any = pallet
                            context.onComplete(result)
                        }
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Pallet
    }
}