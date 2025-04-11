package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

class ProductStatusFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        // Получаем текущий товар из типизированной модели
        val storageProduct = wizardContext.results.storageProduct

        // Устанавливаем начальный статус
        var selectedStatus by remember {
            mutableStateOf(storageProduct?.status ?: ProductStatus.STANDARD)
        }

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

            // Радио-кнопки для выбора статуса
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ProductStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { selectedStatus = status },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (status) {
                                ProductStatus.STANDARD -> "Кондиция (стандарт)"
                                ProductStatus.DEFECTIVE -> "Брак"
                                ProductStatus.EXPIRED -> "Просрочен"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка подтверждения
            Button(
                onClick = {
                    if (storageProduct != null) {
                        // Обновляем существующий продукт с новым статусом
                        val updatedProduct = storageProduct.copy(status = selectedStatus)
                        // Используем типизированный метод
                        wizardContext.completeWithStorageProduct(updatedProduct)
                    } else {
                        // Если нет продукта, просто возвращаем статус
                        wizardContext.onComplete(selectedStatus)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Подтвердить")
            }
        }
    }
}