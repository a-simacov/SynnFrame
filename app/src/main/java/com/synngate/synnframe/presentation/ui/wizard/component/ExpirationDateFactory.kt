package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Фабрика для шага ввода срока годности
 */
class ExpirationDateFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        val storageProduct = wizardContext.results["STORAGE_PRODUCT"] as? TaskProduct

        // Выбираем начальную дату: или из существующего продукта, или +30 дней от текущей
        val initialDate = storageProduct?.takeIf { it.hasExpirationDate() }?.expirationDate
            ?: LocalDate.now().plusDays(30)

        var selectedDate by remember { mutableStateOf(initialDate) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Показать информацию о выбранном товаре
            storageProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Упрощенный компонент выбора даты (в реальности здесь был бы DatePicker)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбранная дата: ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки для манипуляции датой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedDate = selectedDate.minusDays(1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-1 день")
                }

                Button(
                    onClick = { selectedDate = selectedDate.plusDays(1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 день")
                }

                Button(
                    onClick = { selectedDate = selectedDate.plusMonths(1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+1 месяц")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Кнопка "Пропустить"
                OutlinedButton(
                    onClick = {
                        if (storageProduct != null) {
                            // Если есть продукт, обновляем без срока годности
                            wizardContext.onComplete(
                                storageProduct.copy(expirationDate = LocalDate.of(1970, 1, 1))
                            )
                        } else {
                            // Если нет продукта, просто идем дальше
                            wizardContext.onComplete(null)
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Пропустить")
                }

                // Кнопка "Подтвердить"
                Button(
                    onClick = {
                        if (storageProduct != null) {
                            // Обновляем существующий продукт с новой датой
                            val updatedProduct = storageProduct.copy(expirationDate = selectedDate)
                            wizardContext.onComplete(updatedProduct)
                        } else {
                            // Если нет продукта, просто возвращаем дату
                            wizardContext.onComplete(selectedDate)
                        }
                    }
                ) {
                    Text("Подтвердить")
                }
            }
        }
    }
}