package com.synngate.synnframe.presentation.ui.wizard.action.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.ProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardStepUtils
import com.synngate.synnframe.presentation.ui.wizard.service.ProductLookupService

/**
 * Обновленная фабрика для шага выбора продукта с использованием стандартных компонентов
 */
class ProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<Product>(), AutoCompleteCapableFactory {

    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<Product> {
        return ProductSelectionViewModel(
            step = step,
            action = action,
            context = context,
            productLookupService = productLookupService,
            validationService = validationService
        )
    }

    @Composable
    override fun StepContent(
        state: StepViewState<Product>,
        viewModel: BaseStepViewModel<Product>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val productViewModel = viewModel as? ProductSelectionViewModel

        if (productViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        // Обработка штрих-кода из контекста
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
                    productViewModel.hideProductSelectionDialog()
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

        // Используем стандартный контейнер для шага
        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            context = context,
            forwardEnabled = productViewModel.hasSelectedProduct(),
            content = {
                ProductSelectionContent(
                    state = state,
                    viewModel = productViewModel
                )
            }
        )
    }

    @Composable
    private fun ProductSelectionContent(
        state: StepViewState<Product>,
        viewModel: ProductSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Отображаем список товаров из плана, если они есть
            if (viewModel.hasPlanProducts()) {
                WizardStepUtils.ProductSelectionList(
                    products = viewModel.getPlanProducts(),
                    onProductSelect = { taskProduct ->
                        viewModel.selectProductFromPlan(taskProduct)
                    },
                    selectedProductId = viewModel.getSelectedProduct()?.id,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)
            }

            // ИЗМЕНЕНИЕ: Показываем поле поиска только если нет плана
            // или выбранный товар не соответствует плану, или товар еще не выбран
            if (!viewModel.hasPlanProducts() ||
                !viewModel.isSelectedProductMatchingPlan() ||
                viewModel.getSelectedProduct() == null) {

                // Используем стандартное поле ввода штрих-кода
                WizardStepUtils.StandardBarcodeField(
                    value = viewModel.barcodeInput,
                    onValueChange = { viewModel.updateBarcodeInput(it) },
                    onSearch = { viewModel.searchByBarcode() },
                    onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                    isError = state.error != null,
                    errorText = state.error,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(8)

                // Кнопка выбора из списка только если нет товаров в плане
                if (!viewModel.hasPlanProducts()) {
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

                FormSpacer(8)
            }

            // Отображаем выбранный продукт, если он не из плана
            val selectedProduct = viewModel.getSelectedProduct()
            if (selectedProduct != null && !viewModel.isSelectedProductMatchingPlan()) {
                ProductCard(
                    product = selectedProduct,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is Product
    }

    // Реализация интерфейса AutoCompleteCapableFactory

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedProduct" // Имя поля, при изменении которого происходит автопереход
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        // Включаем автопереход для шагов выбора товара из плана
        return step.promptText.contains("план", ignoreCase = true)
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        // Для выбора товара не требуется подтверждение перед автопереходом
        return false
    }
}