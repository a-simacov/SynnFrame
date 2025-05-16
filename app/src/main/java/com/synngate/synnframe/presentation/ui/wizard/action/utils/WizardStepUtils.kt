package com.synngate.synnframe.presentation.ui.wizard.action.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
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
import timber.log.Timber

/**
 * Утилитный класс для помощи фабрикам шагов в использовании стандартных компонентов
 */
object WizardStepUtils {

    /**
     * Создает стандартный контейнер для шага
     */
    @Composable
    fun <T> StandardStepContainer(
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
            // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: используем специальный обработчик для шага ввода количества
            onForward = {
                // Если это шаг ввода количества, вызываем saveResult()
                if (viewModel is ProductQuantityViewModel && state.data != null) {
                    Timber.d("StandardStepContainer: detected quantity step, using direct saveResult()")
                    viewModel.saveResult()
                } else {
                    // Иначе используем стандартный onForward
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

    /**
     * Отображает список планируемых продуктов с выбором
     */
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

    /**
     * Отображает список паллет для выбора
     */
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

    /**
     * Отображает список ячеек для выбора
     */
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

    /**
     * Отображает стандартное поле для сканирования штрих-кода
     */
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
        placeholder: String? = null
    ) {
        WizardBarcodeField(
            value = value,
            onValueChange = onValueChange,
            onSearch = onSearch,
            onScannerClick = onScannerClick,
            modifier = modifier,
            label = label,
            isError = isError,
            errorText = errorText,
            onSelectFromList = onSelectFromList,
            placeholder = placeholder
        )
    }

    /**
     * Отображает экран ошибки, если ViewModel не удалось создать или привести к нужному типу
     */
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

    fun hasApiValidation(step: ActionStep): Boolean {
        return step.validationRules.rules.any {
            it.type == ValidationType.API_REQUEST && it.apiEndpoint != null
        }
    }
}