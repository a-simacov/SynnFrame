package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExpirationDateFactory(
    private val wizardViewModel: FactLineWizardViewModel
) : StepComponentFactory {
    @Composable
    override fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    ) {
        // Получаем текущий товар из типизированной модели
        val storageProduct = wizardContext.results.storageProduct

        // Выбираем начальную дату: или из существующего продукта, или +30 дней от текущей
        val initialDate = storageProduct?.takeIf { it.hasExpirationDate() }?.expirationDate
            ?: LocalDate.now().plusDays(30)

        var selectedDate by remember { mutableStateOf(initialDate) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Показать информацию о выбранном товаре
            storageProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Компонент выбора даты
            DatePickerView(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Кнопка "Пропустить"
                OutlinedButton(
                    onClick = {
                        if (storageProduct != null) {
                            // Используем дату-заглушку, но не null
                            val updatedProduct = storageProduct.copy(
                                expirationDate = LocalDate.of(1970, 1, 1)
                            )
                            // Используем типизированный метод
                            wizardContext.completeWithStorageProduct(updatedProduct)
                        } else {
                            // Просто идем дальше
                            wizardContext.onSkip(LocalDate.of(1970, 1, 1))
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Пропустить")
                }

                // Кнопка "Подтвердить"
                Button(
                    onClick = {
                        if (storageProduct != null) {
                            // Обновляем существующий продукт с новой датой
                            val updatedProduct = storageProduct.copy(
                                expirationDate = selectedDate
                            )
                            // Используем типизированный метод
                            wizardContext.completeWithStorageProduct(updatedProduct)
                        } else {
                            // Просто передаем выбранную дату
                            wizardContext.onComplete(selectedDate)
                        }
                    }
                ) {
                    Text("Подтвердить")
                }
            }
        }
    }

    override fun validateStepResult(action: FactLineXAction, results: WizardResultModel): Boolean {
        // Получаем выбранный товар из типизированной модели
        val product = results.storageProduct

        // Проверяем, учитывается ли товар по срокам годности
        val accountingModel = product?.product?.accountingModel
        val needsExpirationDate = accountingModel == AccountingModel.BATCH

        // Если не требуется учет по срокам, считаем шаг "выполненным"
        if (!needsExpirationDate) return true

        // Иначе проверяем, установлена ли дата
        return product?.hasExpirationDate() ?: false
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun DatePickerView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Кнопка для отображения текущей выбранной даты
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Выбрать дату"
                )
            }
        }

        // Кнопки для быстрой корректировки даты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onDateSelected(selectedDate.minusDays(1)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("-1 день")
            }

            Button(
                onClick = { onDateSelected(selectedDate.plusDays(1)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1 день")
            }

            Button(
                onClick = { onDateSelected(selectedDate.plusMonths(1)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1 месяц")
            }
        }

        // Диалог выбора даты
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { dateMillis ->
                                val localDate = java.time.Instant.ofEpochMilli(dateMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateSelected(localDate)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDatePicker = false }) {
                        Text("Отмена")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}