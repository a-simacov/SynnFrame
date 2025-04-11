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
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика для шага закрытия паллеты
 */
class PalletClosingFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var isClosing by remember { mutableStateOf(false) }

        // Получаем текущую паллету из промежуточных результатов
        val pallet = findPalletFromResults(wizardContext.results, groupContext)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            pallet?.let {
                Text(
                    text = "Паллета: ${it.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Статус: ${if (it.isClosed) "Закрыта" else "Открыта"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    isClosing = true
                    pallet?.let { currentPallet ->
                        wizardViewModel.closePallet(currentPallet.code) { result ->
                            isClosing = false
                            // Если успешно, возвращаем обновленную паллету
                            if (result.isSuccess) {
                                val updatedPallet = currentPallet.copy(isClosed = true)
                                wizardContext.onComplete(updatedPallet)
                            } else {
                                // В случае ошибки просто скипаем шаг,
                                // возвращая текущую паллету
                                wizardContext.onComplete(currentPallet)
                            }
                        }
                    } ?: run {
                        // Если паллета не найдена, просто идем дальше
                        isClosing = false
                        wizardContext.onComplete(null)
                    }
                },
                enabled = !isClosing && pallet != null && !pallet.isClosed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isClosing) "Закрытие..." else "Закрыть паллету")
            }
        }
    }

    /**
     * Ищет паллету в результатах в зависимости от типа целевого поля группы
     */
    private fun findPalletFromResults(results: Map<String, Any?>, group: FactLineActionGroup): Pallet? {
        // Ищем поле паллеты в зависимости от целевого поля группы
        val palletKey = when(group.targetFieldType.toString()) {
            "PLACEMENT_PALLET" -> "PLACEMENT_PALLET"
            "STORAGE_PALLET" -> "STORAGE_PALLET"
            else -> null
        }

        // Ищем палету по ключу если он определен
        return if (palletKey != null) {
            // Ищем конкретно по ключу
            results[palletKey] as? Pallet
        } else {
            // Или просто первую паллету в результатах
            results.values.filterIsInstance<Pallet>().firstOrNull()
        }
    }
}