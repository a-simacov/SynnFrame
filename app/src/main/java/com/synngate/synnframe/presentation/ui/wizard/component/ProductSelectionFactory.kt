package com.synngate.synnframe.presentation.ui.wizard.component

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
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.ObjectSelectionCondition
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerView
import com.synngate.synnframe.presentation.ui.taskx.components.ProductItem
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel

class ProductSelectionFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        // Получаем продукты через ViewModel
        val products by wizardViewModel.products.collectAsState()

        // Загрузка продуктов при изменении поискового запроса
        LaunchedEffect(action.selectionCondition, searchQuery) {
            // Получаем IDs продуктов из плана, если нужно
            val planProductIds = if (action.selectionCondition == ObjectSelectionCondition.FROM_PLAN) {
                wizardContext.stepResults["PLAN_PRODUCT_IDS"] as? Set<String>
            } else {
                null
            }

            wizardViewModel.loadProducts(searchQuery, planProductIds)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        wizardViewModel.findProductByBarcode(barcode) { product ->
                            if (product != null) {
                                // Создаем TaskProduct из найденного продукта
                                val taskProduct = TaskProduct(
                                    product = product,
                                    quantity = 1f,
                                    status = ProductStatus.STANDARD
                                )
                                // Используем метод completeWithStorageProduct
                                wizardContext.completeWithStorageProduct(taskProduct)
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
                                val taskProduct = TaskProduct(
                                    product = product,
                                    quantity = 1f,
                                    status = ProductStatus.STANDARD
                                )
                                // Используем метод для обновления продукта хранения
                                wizardContext.completeWithStorageProduct(taskProduct)
                            }
                        )
                    }
                }
            }
        }
    }
}