package com.synngate.synnframe.presentation.ui.wizard.action

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
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.taskx.components.ProductItem
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

/**
 * Фабрика компонентов для шага выбора продукта
 */
class ProductSelectionStepFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        // Получаем продукты через ViewModel
        val products by wizardViewModel.products.collectAsState()

        // Загрузка продуктов при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            // Получаем IDs продуктов из плана, если нужно
            val planItems = context.results["planItems"] as? List<*>
            val planProductIds = planItems
                ?.filterIsInstance<TaskProduct>()
                ?.map { it.product.id }
                ?.toSet()

            wizardViewModel.loadProducts(searchQuery, planProductIds)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        wizardViewModel.findProductByBarcode(barcode) { product ->
                            if (product != null) {
                                // Создаем TaskProduct из найденного продукта или возвращаем сам Product
                                // в зависимости от типа объекта
                                val result = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                    TaskProduct(product = product)
                                } else {
                                    product
                                }

                                context.onComplete(result)
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
                    label = { Text("Поиск товара") },
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
                    items(products) { product ->
                        ProductItem(
                            product = product,
                            onClick = {
                                // Создаем результат в зависимости от типа объекта
                                val result = if (step.objectType == ActionObjectType.TASK_PRODUCT) {
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