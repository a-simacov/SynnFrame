package com.synngate.synnframe.presentation.ui.wizard.action.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.BarcodeEntryField
import com.synngate.synnframe.presentation.ui.wizard.action.components.PlanProductsList
import com.synngate.synnframe.presentation.ui.wizard.action.components.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.StepContainer
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService
import timber.log.Timber

/**
 * Фабрика для шага выбора продукта
 */
class ProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<Product>() {

    /**
     * Создание ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<Product> {
        return ProductSelectionViewModel(
            step = step,
            action = action,
            context = context,
            productLookupService = productLookupService,
            validationService = validationService
        )
    }

    /**
     * Отображение UI для шага
     */
    @Composable
    override fun StepContent(
        state: StepViewState<Product>,
        viewModel: BaseStepViewModel<Product>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val productViewModel = try {
            viewModel as ProductSelectionViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к ProductSelectionViewModel")
            null
        }

        // Если приведение не удалось, показываем сообщение об ошибке
        if (productViewModel == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ошибка инициализации шага. Пожалуйста, вернитесь назад и попробуйте снова.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        // Обработка штрих-кода из контекста, если он есть
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                productViewModel.processBarcode(barcode)
            }
        }

        // Диалог сканирования
        if (productViewModel.showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    productViewModel.processBarcode(barcode)
                    productViewModel.hideCameraScannerDialog()
                },
                onClose = {
                    productViewModel.hideCameraScannerDialog()
                },
                instructionText = stringResource(R.string.scan_product)
            )
        }

        // Диалог выбора продукта
        if (productViewModel.showProductSelectionDialog) {
            OptimizedProductSelectionDialog(
                onProductSelected = { product ->
                    productViewModel.selectProduct(product)
                },
                onDismiss = {
                    productViewModel.hideProductSelectionDialog()
                },
                initialFilter = "",
                title = "Выберите товар",
                planProductIds = if (productViewModel.hasPlanProducts()) {
                    productViewModel.getPlanProducts().map { it.product.id }.toSet()
                } else null
            )
        }

        // Используем StepContainer для унифицированного отображения шага
        StepContainer(
            state = state,
            step = step,
            action = action,
            onBack = { context.onBack() },
            onForward = {
                // Безопасное использование выбранного продукта
                productViewModel.getSelectedProduct()?.let { product ->
                    productViewModel.completeStep(product)
                }
            },
            onCancel = { context.onCancel() },
            forwardEnabled = productViewModel.hasSelectedProduct(),
            content = {
                SafeProductSelectionContent(
                    state = state,
                    viewModel = productViewModel
                )
            }
        )
    }

    /**
     * Безопасное содержимое шага выбора продукта
     */
    @Composable
    private fun SafeProductSelectionContent(
        state: StepViewState<Product>,
        viewModel: ProductSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Поле ввода штрих-кода
            BarcodeEntryField(
                value = viewModel.barcodeInput,
                onValueChange = { viewModel.updateBarcodeInput(it) },
                onSearch = { viewModel.searchByBarcode() },
                onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                isError = state.error != null,
                errorText = state.error,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранного продукта
            val selectedProduct = viewModel.getSelectedProduct()
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный продукт:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProductCard(
                    product = selectedProduct,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображение продуктов из плана
            if (viewModel.hasPlanProducts()) {
                PlanProductsList(
                    planProducts = viewModel.getPlanProducts(),
                    onProductSelect = { taskProduct ->
                        viewModel.selectProductFromPlan(taskProduct)
                    }
                )
            }

            // Кнопка выбора из списка
            Button(
                onClick = { viewModel.toggleProductSelectionDialog(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Выбрать из списка товаров")
            }
        }
    }

    /**
     * Валидация результата шага
     */
    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Product
    }
}