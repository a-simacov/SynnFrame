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
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
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
        var showScanner by remember { mutableStateOf(false) }

        // Получение данных из ViewModel
        val pallets by wizardViewModel.pallets.collectAsState()

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

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        wizardViewModel.findPalletByCode(barcode) { pallet ->
                            if (pallet != null) {
                                context.onComplete(pallet)
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
                    label = { Text("Поиск паллеты") },
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
                    items(pallets) { pallet ->
                        PalletItem(
                            pallet = pallet,
                            onClick = {
                                context.onComplete(pallet)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Pallet
    }
}