package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.taskx.components.BinItem
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика компонентов для шага выбора ячейки
 */
class BinSelectionStepFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        // Получаем зону из контекста, если указана
        val zoneFilter = action.actionTemplate.placementObjectType?.name

        // Получение данных из ViewModel
        val bins by wizardViewModel.bins.collectAsState()

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

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        wizardViewModel.findBinByCode(barcode) { bin ->
                            if (bin != null) {
                                context.onComplete(bin)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Button(
                    onClick = { showScanner = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Выбрать из списка")
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск ячейки") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Сканировать штрихкод")
                }

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(bins) { bin ->
                        BinItem(
                            bin = bin,
                            onClick = {
                                context.onComplete(bin)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is BinX
    }
}