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
import com.synngate.synnframe.domain.service.TaskContextManager
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

class ProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService,
    taskContextManager: TaskContextManager?
) : BaseActionStepFactory<Product>(taskContextManager), AutoCompleteCapableFactory {

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
            validationService = validationService,
            taskContextManager = taskContextManager
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
        val productViewModel = viewModel as? ProductSelectionViewModel

        if (productViewModel == null) {
            WizardStepUtils.ViewModelErrorScreen()
            return
        }

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                productViewModel.processBarcode(barcode)
            }
        }

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

        WizardStepUtils.StandardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = productViewModel,
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

            if (!viewModel.hasPlanProducts() ||
                !viewModel.isSelectedProductMatchingPlan() ||
                viewModel.getSelectedProduct() == null) {

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

    override fun getAutoCompleteFieldName(step: ActionStep): String? {
        return "selectedProduct"
    }

    override fun isAutoCompleteEnabled(step: ActionStep): Boolean {
        return true
    }

    override fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean {
        return false
    }
}