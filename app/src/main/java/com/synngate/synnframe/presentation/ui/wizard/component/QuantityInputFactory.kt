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
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.common.inputs.NumberTextField
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика для шага ввода количества
 */
class QuantityInputFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var quantity by remember { mutableStateOf("1.0") }
        var isError by remember { mutableStateOf(false) }

        // Получаем текущий товар из промежуточных результатов
        val taskProduct = wizardContext.results["STORAGE_PRODUCT"] as? TaskProduct

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            taskProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Артикул: ${product.product.articleNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            NumberTextField(
                value = quantity,
                onValueChange = {
                    quantity = it
                    isError = false
                },
                label = "Количество",
                isError = isError,
                errorText = if (isError) "Введите корректное значение" else null,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    try {
                        val numberValue = quantity.toFloat()
                        if (numberValue > 0) {
                            // Обновляем текущий TaskProduct с новым количеством
                            if (taskProduct != null) {
                                val updatedProduct = taskProduct.copy(quantity = numberValue)
                                wizardContext.onComplete(updatedProduct)
                            } else {
                                // Если нет TaskProduct, просто возвращаем число
                                wizardContext.onComplete(numberValue)
                            }
                        } else {
                            isError = true
                        }
                    } catch (e: Exception) {
                        isError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Подтвердить")
            }
        }
    }
}