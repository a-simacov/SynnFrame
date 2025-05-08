package com.synngate.synnframe.presentation.ui.wizard.action

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

class ProductQuantityStepFactory : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        val previousStepResult = findPreviousStepResult(context)
        val selectedProduct = previousStepResult as? TaskProduct

        if (selectedProduct == null) {
            ErrorScreen(message = "Сначала необходимо выбрать товар")
            return
        }

        val initialValue = remember {
            if (context.hasStepResult) {
                val currentResult = context.getCurrentStepResult() as? TaskProduct
                currentResult?.quantity?.toString() ?: "1"
            } else {
                "1"
            }
        }

        var quantity by remember { mutableStateOf(initialValue) }
        var errorMessage by remember(context.validationError) {
            mutableStateOf(context.validationError)
        }

        val plannedProduct = action.storageProduct
        val plannedQuantity = if (plannedProduct?.product?.id == selectedProduct.product.id) {
            plannedProduct.quantity
        } else {
            0f
        }

        val relatedFactActions = action.getRelatedFactActions(context)

        val completedQuantity = relatedFactActions.sumOf {
            it.storageProduct?.quantity?.toDouble() ?: 0.0
        }.toFloat()

        val remainingQuantity = (plannedQuantity - completedQuantity).coerceAtLeast(0f)

        val currentInputQuantity = quantity.toFloatOrNull() ?: 0f
        val projectedTotalQuantity = completedQuantity + currentInputQuantity

        val willExceedPlan = plannedQuantity > 0f && projectedTotalQuantity > plannedQuantity

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (errorMessage != null) {
                ValidationErrorMessage(
                    message = errorMessage!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SelectedProductCard(
                product = selectedProduct,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            QuantityInfoCard(
                plannedQuantity = plannedQuantity,
                completedQuantity = completedQuantity,
                remainingQuantity = remainingQuantity,
                projectedTotal = projectedTotalQuantity,
                willExceedPlan = willExceedPlan,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val quantityValue = quantity.toFloatOrNull() ?: 0f
                    if (quantityValue <= 0f) {
                        errorMessage = "Количество должно быть больше нуля"
                    } else {
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

    private fun PlannedAction.getRelatedFactActions(context: ActionContext): List<com.synngate.synnframe.domain.entity.taskx.action.FactAction> {
        val factActionsInfo = context.results["factActions"] as? Map<*, *> ?: emptyMap<String, Any>()

        @Suppress("UNCHECKED_CAST")
        return (factActionsInfo[id] as? List<com.synngate.synnframe.domain.entity.taskx.action.FactAction>) ?: emptyList()
    }

    private fun findPreviousStepResult(context: ActionContext): Any? {
        for ((stepId, value) in context.results) {
            if (stepId != context.stepId && value is TaskProduct) {
                return value
            }
        }

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
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = product.product.name,
                    style = MaterialTheme.typography.titleLarge,
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
        completedQuantity: Float,
        remainingQuantity: Float,
        projectedTotal: Float,
        willExceedPlan: Boolean,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                QuantityRow(
                    label = "Запланировано:",
                    value = plannedQuantity.toString()
                )

                QuantityRow(
                    label = "Выполнено:",
                    value = completedQuantity.toString()
                )

                QuantityRow(
                    label = "Осталось:",
                    value = remainingQuantity.toString(),
                    highlight = true
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                QuantityRow(
                    label = "Текущий итог:",
                    value = projectedTotal.toString(),
                    highlight = true,
                    warning = willExceedPlan
                )

                if (willExceedPlan) {
                    Text(
                        text = "Внимание: превышение планового количества!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        val taskProduct = value as? TaskProduct
        return taskProduct != null && taskProduct.quantity > 0
    }
}

@Composable
fun QuantityRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    warning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (warning)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}