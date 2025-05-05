package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField

/**
 * Фабрика для создания компонента шага ввода количества товара.
 * Этот шаг предполагает, что товар уже был выбран на предыдущем шаге.
 */
class ProductQuantityStepFactory : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Находим выбранный продукт из предыдущего шага
        val previousStepResult = findPreviousStepResult(context)
        val selectedProduct = previousStepResult as? TaskProduct

        if (selectedProduct == null) {
            // Если продукт не найден, показываем ошибку
            ErrorScreen(message = "Сначала необходимо выбрать товар")
            return
        }

        // Состояние для количества
        var quantity by remember { mutableStateOf("1") }
        var errorMessage by remember(context.validationError) {
            mutableStateOf(context.validationError)
        }

        // Получаем плановое количество
        val plannedProduct = action.storageProduct
        val plannedQuantity = if (plannedProduct?.product?.id == selectedProduct.product.id) {
            plannedProduct.quantity
        } else {
            0f
        }

        // Вычисляем оставшееся количество (в будущем можно будет учитывать уже выполненные действия)
        val remainingQuantity = plannedQuantity

        // Инициализация количества из планового, если еще не задано
        LaunchedEffect(selectedProduct) {
            if (quantity == "1" && plannedQuantity > 0) {
                quantity = plannedQuantity.toString()
            }
        }

        // Основной интерфейс
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображение ошибки валидации, если есть
            if (errorMessage != null) {
                ValidationErrorMessage(message = errorMessage!!)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Информация о выбранном товаре
            SelectedProductCard(
                product = selectedProduct,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Информация о количестве (плановом и оставшемся)
            QuantityInfoCard(
                plannedQuantity = plannedQuantity,
                remainingQuantity = remainingQuantity,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Поле ввода количества
            QuantityTextField(
                value = quantity,
                onValueChange = { newValue ->
                    quantity = newValue
                    errorMessage = null
                },
                label = "Введите количество",
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                errorText = errorMessage
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопка подтверждения
            Button(
                onClick = {
                    val quantityValue = quantity.toFloatOrNull() ?: 0f
                    if (quantityValue <= 0f) {
                        errorMessage = "Количество должно быть больше нуля"
                    } else {
                        // Создаем обновленный TaskProduct с указанным количеством
                        val updatedProduct = selectedProduct.copy(quantity = quantityValue)
                        context.onComplete(updatedProduct)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = quantity.isNotEmpty()
            ) {
                Text("Подтвердить")
            }
        }
    }

    /**
     * Поиск результата предыдущего шага (выбора товара)
     */
    private fun findPreviousStepResult(context: ActionContext): Any? {
        // Находим все шаги, выполненные перед текущим
        val allStepIds = context.results.keys

        // Ищем результат типа TaskProduct из предыдущих шагов
        for ((stepId, value) in context.results) {
            if (stepId != context.stepId && value is TaskProduct) {
                return value
            }
        }

        // Если не нашли TaskProduct, ищем Product
        for ((stepId, value) in context.results) {
            if (stepId != context.stepId && value is Product) {
                return TaskProduct(product = value, quantity = 0f)
            }
        }

        return null
    }

    @Composable
    private fun ErrorScreen(message: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun ValidationErrorMessage(message: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Ошибка валидации",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    private fun SelectedProductCard(
        product: TaskProduct,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Выбранный товар",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Артикул: ${product.product.articleNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    @Composable
    private fun QuantityInfoCard(
        plannedQuantity: Float,
        remainingQuantity: Float,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Информация о количестве",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Плановое количество:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = plannedQuantity.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Осталось выполнить:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = remainingQuantity.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Проверяем, что результат - это TaskProduct и количество больше 0
        val taskProduct = value as? TaskProduct
        return taskProduct != null && taskProduct.quantity > 0
    }
}