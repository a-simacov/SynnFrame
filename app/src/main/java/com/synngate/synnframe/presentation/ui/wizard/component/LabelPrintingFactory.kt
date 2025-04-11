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
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика для шага печати этикетки
 */
class LabelPrintingFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var isPrinting by remember { mutableStateOf(false) }

        // Получаем паллету в зависимости от типа группы
        val pallet = when(groupContext.targetFieldType.toString()) {
            "PLACEMENT_PALLET" -> wizardContext.results["PLACEMENT_PALLET"] as? Pallet
            "STORAGE_PALLET" -> wizardContext.results["STORAGE_PALLET"] as? Pallet
            else -> wizardContext.results.values.filterIsInstance<Pallet>().firstOrNull()
        }

        // Получаем товар, если нет паллеты
        val product = wizardContext.results["STORAGE_PRODUCT"] as? TaskProduct

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
                            wizardContext.onComplete(result.isSuccess)
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
}