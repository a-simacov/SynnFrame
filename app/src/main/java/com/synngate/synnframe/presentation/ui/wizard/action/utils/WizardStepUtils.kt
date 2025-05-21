package com.synngate.synnframe.presentation.ui.wizard.action.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.components.AutoFillIndicator
import com.synngate.synnframe.presentation.ui.wizard.action.components.FormSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.StateType
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardBarcodeField
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardEmptyState
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardStateMessage
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardStepContainer
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.BinCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.PalletCard
import com.synngate.synnframe.presentation.ui.wizard.action.components.adapters.TaskProductCard
import com.synngate.synnframe.presentation.ui.wizard.action.quantity.ProductQuantityViewModel

object WizardStepUtils {

    @Composable
    fun <T:Any> StandardStepContainer(
        state: StepViewState<T>,
        step: ActionStep,
        action: PlannedAction,
        viewModel: BaseStepViewModel<T>,
        context: ActionContext,
        forwardEnabled: Boolean,
        content: @Composable () -> Unit
    ) {
        WizardStepContainer(
            state = state,
            step = step,
            action = action,
            viewModel = viewModel,
            onBack = { context.onBack() },
            onForward = {
                if (viewModel is ProductQuantityViewModel && state.data != null) {
                    viewModel.saveResult()
                } else {
                    context.onForward()
                }
            },
            onCancel = { context.onCancel() },
            forwardEnabled = forwardEnabled,
            isProcessingGlobal = context.isProcessingStep,
            isFirstStep = context.isFirstStep,
            content = content
        )
    }

    @Composable
    fun ProductSelectionList(
        products: List<TaskProduct>,
        onProductSelect: (TaskProduct) -> Unit,
        selectedProductId: String? = null,
        modifier: Modifier = Modifier
    ) {
        if (products.isEmpty()) {
            WizardEmptyState(
                message = "Нет продуктов в плане",
                icon = Icons.Default.Info,
                modifier = modifier
            )
            return
        }

        Column(modifier = modifier.fillMaxWidth()) {
            products.forEach { taskProduct ->
                val isSelected = selectedProductId == taskProduct.product.id

                TaskProductCard(
                    taskProduct = taskProduct,
                    onClick = { onProductSelect(taskProduct) },
                    isSelected = isSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(4)
            }
        }
    }

    @Composable
    fun PalletList(
        pallets: List<Pallet>,
        onPalletSelect: (Pallet) -> Unit,
        selectedPalletCode: String? = null,
        modifier: Modifier = Modifier
    ) {
        if (pallets.isEmpty()) {
            WizardEmptyState(
                message = "Список паллет пуст",
                icon = Icons.Default.Info,
                modifier = modifier
            )
            return
        }

        Column(modifier = modifier.fillMaxWidth()) {
            pallets.forEach { pallet ->
                val isSelected = selectedPalletCode == pallet.code

                PalletCard(
                    pallet = pallet,
                    onClick = { onPalletSelect(pallet) },
                    isSelected = isSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(4)
            }
        }
    }

    @Composable
    fun BinList(
        bins: List<BinX>,
        onBinSelect: (BinX) -> Unit,
        selectedBinCode: String? = null,
        modifier: Modifier = Modifier
    ) {
        if (bins.isEmpty()) {
            WizardEmptyState(
                message = "Список ячеек пуст",
                icon = Icons.Default.Info,
                modifier = modifier
            )
            return
        }

        Column(modifier = modifier.fillMaxWidth()) {
            bins.forEach { bin ->
                val isSelected = selectedBinCode == bin.code

                BinCard(
                    bin = bin,
                    onClick = { onBinSelect(bin) },
                    isSelected = isSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSpacer(4)
            }
        }
    }

    @Composable
    fun StandardBarcodeField(
        value: String,
        onValueChange: (String) -> Unit,
        onSearch: () -> Unit,
        onScannerClick: () -> Unit,
        modifier: Modifier = Modifier,
        label: String = "Введите штрихкод",
        isError: Boolean = false,
        errorText: String? = null,
        onSelectFromList: (() -> Unit)? = null,
        placeholder: String? = null,
        isAutoFilled: Boolean = false
    ) {
        Column(modifier = modifier) {
            WizardBarcodeField(
                value = value,
                onValueChange = onValueChange,
                onSearch = onSearch,
                onScannerClick = onScannerClick,
                modifier = Modifier.fillMaxWidth(),
                label = label,
                isError = isError,
                errorText = errorText,
                onSelectFromList = onSelectFromList,
                placeholder = placeholder
            )

            if (isAutoFilled) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    AutoFillIndicator()
                }
            }
        }
    }

    @Composable
    fun ViewModelErrorScreen(
        message: String = "Ошибка инициализации шага. Пожалуйста, вернитесь назад и попробуйте снова."
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WizardStateMessage(
                    message = message,
                    type = StateType.ERROR
                )

                FormSpacer(16)

                Text(
                    text = "Для решения проблемы вернитесь назад и повторите действие",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun AutoFilledField(
        label: String,
        value: String,
        source: String? = null,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.width(8.dp))

                AutoFillIndicator(source = source)
            }
        }
    }
}