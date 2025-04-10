package com.synngate.synnframe.presentation.ui.wizard.component

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
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.taskx.components.PalletItem
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import timber.log.Timber

class PalletSelectionFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        // Получение данных из ViewModel
        val pallets by wizardViewModel.pallets.collectAsState()

        // Загрузка паллет при изменении поискового запроса
        LaunchedEffect(action.selectionCondition, searchQuery) {
            wizardViewModel.loadPallets(searchQuery)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        wizardViewModel.findPalletByCode(barcode) { pallet ->
                            if (pallet != null) {
                                // Определяем, куда записать паллету на основе targetFieldType группы
                                when (groupContext.targetFieldType) {
                                    TaskXLineFieldType.STORAGE_PALLET ->
                                        wizardContext.completeWithStoragePallet(pallet)
                                    TaskXLineFieldType.PLACEMENT_PALLET ->
                                        wizardContext.completeWithPlacementPallet(pallet)
                                    else -> {
                                        Timber.w("Неизвестный тип целевого поля: ${groupContext.targetFieldType}")
                                        wizardContext.onComplete(pallet)
                                    }
                                }
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
                                // Определяем, куда записать паллету на основе targetFieldType группы
                                when (groupContext.targetFieldType) {
                                    TaskXLineFieldType.STORAGE_PALLET ->
                                        wizardContext.completeWithStoragePallet(pallet)
                                    TaskXLineFieldType.PLACEMENT_PALLET ->
                                        wizardContext.completeWithPlacementPallet(pallet)
                                    else -> {
                                        Timber.w("Неизвестный тип целевого поля: ${groupContext.targetFieldType}")
                                        wizardContext.onComplete(pallet)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun validateStepResult(action: FactLineXAction, results: WizardResultModel): Boolean {
        // В зависимости от целевого поля проверяем соответствующую паллету
        return when (groupContext.targetFieldType) {
            TaskXLineFieldType.STORAGE_PALLET -> results.storagePallet != null
            TaskXLineFieldType.PLACEMENT_PALLET -> results.placementPallet != null
            else -> true // Для других типов не требуем паллету
        }
    }
}