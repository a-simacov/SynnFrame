package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.taskx.components.ProductItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel

/**
 * Фабрика компонентов для шага выбора продукта
 */
class ProductSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }

        // Получаем продукты через ViewModel
        val products by wizardViewModel.products.collectAsState()

        // Получаем запланированные продукты из действия
        val plannedProduct = action.storageProduct?.product

        // Список продуктов из плана для показа пользователю
        val planProducts = remember(action) {
            listOfNotNull(action.storageProduct)
        }

        // Получаем уже выбранный продукт из контекста, если есть
        val selectedProduct = remember(context.results) {
            context.results[step.id] as? Product ?:
            (context.results[step.id] as? TaskProduct)?.product
        }

        // Загрузка продуктов при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            // Получаем IDs продуктов из плана
            val planProductIds = plannedProduct?.let { setOf(it.id) } ?: emptySet()
            wizardViewModel.loadProducts(searchQuery, planProductIds)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображаем ранее выбранный продукт, если есть
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный продукт:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = selectedProduct.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Артикул: ${selectedProduct.articleNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные продукты, если они есть
            if (planProducts.isNotEmpty()) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                ) {
                    items(planProducts) { taskProduct ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = taskProduct.product.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Артикул: ${taskProduct.product.articleNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (taskProduct.quantity > 0) {
                                    Text(
                                        text = "Количество: ${taskProduct.quantity}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = {
                                        // Создаем результат в зависимости от типа объекта
                                        val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                            taskProduct
                                        } else {
                                            taskProduct.product
                                        }
                                        context.onComplete(result)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск товара") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onClick = {
                            // Создаем результат в зависимости от типа объекта
                            val result: Any = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                TaskProduct(product = product)
                            } else {
                                product
                            }

                            context.onComplete(result)
                        }
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Проверяем тип результата
        return when (step.objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> value is Product
            ActionObjectType.TASK_PRODUCT -> value is TaskProduct
            else -> false
        }
    }
}