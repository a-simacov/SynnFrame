package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Артикул: ${product.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BinItem(
    bin: BinX,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${bin.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PalletItem(
    pallet: Pallet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Код: ${pallet.code}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ExpirationDateStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onDateEntered: (LocalDate) -> Unit,
    viewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    val storageProduct = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) } // По умолчанию +30 дней

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
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

        // Компонент выбора даты (упрощенный вариант)
        DatePickerComponent(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            // Кнопка "Пропустить"
            OutlinedButton(
                onClick = { onDateEntered(LocalDate.of(1970, 1, 1)) }, // Дата-заглушка
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Пропустить")
            }

            // Кнопка "Подтвердить"
            Button(
                onClick = { onDateEntered(selectedDate) }
            ) {
                Text("Подтвердить")
            }
        }
    }
}

@Composable
fun DatePickerComponent(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: FactLineWizardViewModel, // Добавляем параметр ViewModel
    modifier: Modifier = Modifier
) {
    // Упрощенный компонент для выбора даты
    // В реальном приложении здесь был бы полноценный DatePicker
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Выбранная дата: ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
    }
}

@Composable
fun ProductStatusStep(
    promptText: String,
    intermediateResults: Map<String, Any?>,
    onStatusSelected: (ProductStatus) -> Unit,
    viewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    val storageProduct = intermediateResults["STORAGE_PRODUCT"] as? TaskProduct
    var selectedStatus by remember { mutableStateOf(ProductStatus.STANDARD) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = promptText,
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

        // Радио-кнопки для выбора статуса
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProductStatus.entries.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedStatus = status },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            ProductStatus.STANDARD -> "Кондиция (стандарт)"
                            ProductStatus.DEFECTIVE -> "Брак"
                            ProductStatus.EXPIRED -> "Просрочен"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка подтверждения
        Button(
            onClick = { onStatusSelected(selectedStatus) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Подтвердить")
        }
    }
}