package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

class LabelPrintingFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    // Сохраняем ссылку на groupContext
    private lateinit var groupContext: FactLineActionGroup

    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        // Сохраняем groupContext для использования в validator
        this.groupContext = groupContext

        var isPrinting by remember { mutableStateOf(false) }

        // Определяем, с какой паллетой работаем, на основе targetFieldType группы
        val pallet = when (groupContext.targetFieldType) {
            TaskXLineFieldType.PLACEMENT_PALLET -> wizardContext.results.placementPallet
            TaskXLineFieldType.STORAGE_PALLET -> wizardContext.results.storagePallet
            else -> {
                // Если тип не определен, берем первую непустую паллету
                wizardContext.results.placementPallet ?: wizardContext.results.storagePallet
            }
        }

        // Получаем товар, если нет паллеты
        val product = wizardContext.results.storageProduct

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            pallet?.let {
                Text(
                    text = "Печать этикетки для паллеты: ${it.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            product?.let {
                Text(
                    text = "Печать этикетки для товара: ${it.product.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    isPrinting = true
                    pallet?.let { currentPallet ->
                        wizardViewModel.printPalletLabel(currentPallet.code) { result ->
                            isPrinting = false
                            // Просто продолжаем с текущей паллетой
                            when (groupContext.targetFieldType) {
                                TaskXLineFieldType.PLACEMENT_PALLET ->
                                    wizardContext.completeWithPlacementPallet(currentPallet)
                                TaskXLineFieldType.STORAGE_PALLET ->
                                    wizardContext.completeWithStoragePallet(currentPallet)
                                else -> wizardContext.onComplete(currentPallet)
                            }
                        }
                    } ?: run {
                        // Если нет паллеты, просто имитируем успешную печать
                        isPrinting = false
                        wizardContext.onComplete(true)
                    }
                },
                enabled = !isPrinting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPrinting) "Печать..." else "Напечатать этикетку")
            }
        }
    }

    override fun validateStepResult(action: FactLineXAction, results: WizardResultModel): Boolean {
        // Для печати этикетки не требуется строгая валидация
        return true
    }
}