// app/src/main/java/com/synngate/synnframe/presentation/ui/wizard/action/taskproduct/TaskProductSelectionStepFactory.kt
package com.synngate.synnframe.presentation.ui.wizard.action.taskproduct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.inputs.ExpirationDatePicker
import com.synngate.synnframe.presentation.common.inputs.ProductStatusSelector
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
 * Фабрика для шага выбора TaskProduct (товара с учетными характеристиками)
 */
class TaskProductSelectionStepFactory(
    private val productLookupService: ProductLookupService,
    private val validationService: ValidationService
) : BaseActionStepFactory<TaskProduct>() {

    /**
     * Создание ViewModel для шага
     */
    override fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<TaskProduct> {
        return TaskProductSelectionViewModel(
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
        state: StepViewState<TaskProduct>,
        viewModel: BaseStepViewModel<TaskProduct>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Безопасное приведение ViewModel к конкретному типу
        val taskProductViewModel = try {
            viewModel as TaskProductSelectionViewModel
        } catch (e: ClassCastException) {
            Timber.e(e, "Ошибка приведения ViewModel к TaskProductSelectionViewModel")
            null
        }

        // Если приведение не удалось, показываем сообщение об ошибке
        if (taskProductViewModel == null) {
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
                taskProductViewModel.processBarcode(barcode)
            }
        }

        // Диалог сканирования
        if (taskProductViewModel.showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    taskProductViewModel.processBarcode(barcode)
                    taskProductViewModel.hideCameraScannerDialog()
                },
                onClose = {
                    taskProductViewModel.hideCameraScannerDialog()
                },
                instructionText = stringResource(R.string.scan_product)
            )
        }

        // Диалог выбора продукта
        if (taskProductViewModel.showProductSelectionDialog) {
            OptimizedProductSelectionDialog(
                onProductSelected = { product ->
                    taskProductViewModel.setSelectedProduct(product)
                    taskProductViewModel.hideProductSelectionDialog()
                },
                onDismiss = {
                    taskProductViewModel.hideProductSelectionDialog()
                },
                initialFilter = "",
                title = "Выберите товар",
                planProductIds = if (taskProductViewModel.hasPlanProducts()) {
                    taskProductViewModel.planProducts.map { it.product.id }.toSet()
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
                taskProductViewModel.saveResult()
            },
            onCancel = { context.onCancel() },
            forwardEnabled = taskProductViewModel.isFormValid(),
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,  // Передаем флаг первого шага
            content = {
                SafeTaskProductSelectionContent(
                    state = state,
                    viewModel = taskProductViewModel
                )
            }
        )
    }

    @Composable
    private fun SafeTaskProductSelectionContent(
        state: StepViewState<TaskProduct>,
        viewModel: TaskProductSelectionViewModel
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Получаем ID выбранного продукта для подсветки в списке
            val selectedProductId = viewModel.getSelectedProduct()?.id

            // Отображаем товары из плана, подсвечивая выбранный
            if (viewModel.hasPlanProducts()) {
                PlanProductsList(
                    planProducts = viewModel.planProducts,
                    onProductSelect = { selectedTaskProduct ->
                        viewModel.selectTaskProductFromPlan(selectedTaskProduct)
                    },
                    selectedProductId = selectedProductId, // Передаем ID для подсветки
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Поле ввода штрихкода с дополнительной кнопкой выбора из списка
            BarcodeEntryField(
                value = viewModel.productCodeInput,
                onValueChange = { viewModel.updateProductCodeInput(it) },
                onSearch = { viewModel.searchByProductCode() },
                onScannerClick = { viewModel.toggleCameraScannerDialog(true) },
                onSelectFromList = { viewModel.toggleProductSelectionDialog(true) }, // Добавлен обработчик
                isError = state.error != null,
                errorText = state.error,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображаем выбранный товар, только если он не из плана
            val selectedProduct = viewModel.getSelectedProduct()

            if (selectedProduct != null) {
                // Показываем выбор статуса и даты независимо от источника товара,
                // но не показываем карточку товара, если он совпадает с плановым

                if (!viewModel.isSelectedProductMatchingPlan()) {
                    Text(
                        text = "Выбранный товар:",
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
                }

                ProductStatusSelector(
                    selectedStatus = viewModel.selectedStatus,
                    onStatusSelected = { viewModel.setSelectedStatus(it) },
                    modifier = Modifier.fillMaxWidth(),
                    isEnabled = !viewModel.isExpirationDateExpired()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedProduct.accountingModel == AccountingModel.BATCH) {
                    ExpirationDatePicker(
                        expirationDate = viewModel.expirationDate,
                        onDateSelected = { viewModel.setExpirationDate(it) },
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        if (value !is TaskProduct) return false

        if (value.product.accountingModel == AccountingModel.BATCH && !value.hasExpirationDate()) {
            return false
        }

        return true
    }
}