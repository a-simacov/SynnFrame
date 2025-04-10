package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.PlanLineX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import java.time.LocalDateTime

@Composable
fun ComparedLinesView(
    task: TaskX,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Сравнение плана и факта",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

        // Вычисляем общий прогресс
        val totalPlan = task.planLines.sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
        val totalFact = task.factLines.sumOf { it.storageProduct?.quantity?.toDouble() ?: 0.0 }
        val progress = if (totalPlan > 0) (totalFact / totalPlan * 100).toInt() else 0

        Text(
            text = "Выполнено: $progress% ($totalFact / $totalPlan)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Объединяем строки плана и факта по товарам
        val comparedLines = compareLines(task.planLines, task.factLines)

        if (comparedLines.isEmpty()) {
            EmptyScreenContent(message = "Нет строк в задании")
        } else {
            LazyColumn {
                items(comparedLines) { line ->
                    ComparedLineItem(line)
                }
            }
        }
    }
}

@Composable
fun PlanLinesView(
    planLines: List<PlanLineX>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Строки плана",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

        if (planLines.isEmpty()) {
            EmptyScreenContent(message = "Нет строк в плане")
        } else {
            LazyColumn {
                items(planLines) { planLine ->
                    PlanLineItem(planLine)
                }
            }
        }
    }
}

@Composable
fun FactLinesView(
    factLines: List<FactLineX>,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Строки факта",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

        if (factLines.isEmpty()) {
            EmptyScreenContent(message = "Нет строк в факте")
        } else {
            LazyColumn {
                items(factLines) { factLine ->
                    FactLineItem(factLine, formatDate)
                }
            }
        }
    }
}

@Composable
fun PlanLineItem(
    planLine: PlanLineX,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Товар
            planLine.storageProduct?.let { product ->
                Text(
                    text = product.product.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Артикул: ${product.product.articleNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Количество: ${product.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Другие поля
            planLine.placementBin?.let { bin ->
                Text(
                    text = "Ячейка: ${bin.code} (${bin.zone})",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            planLine.placementPallet?.let { pallet ->
                Text(
                    text = "Паллета: ${pallet.code}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun FactLineItem(
    factLine: FactLineX,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Товар
            factLine.storageProduct?.let { product ->
                Text(
                    text = product.product.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Артикул: ${product.product.articleNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Количество: ${product.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (product.hasExpirationDate()) {
                    Text(
                        text = "Срок годности: ${product.expirationDate}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Статус: ${product.status}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Другие поля
            factLine.placementBin?.let { bin ->
                Text(
                    text = "Ячейка: ${bin.code} (${bin.zone})",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            factLine.placementPallet?.let { pallet ->
                Text(
                    text = "Паллета: ${pallet.code}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Время
            Text(
                text = "Создано: ${formatDate(factLine.startedAt)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Завершено: ${formatDate(factLine.completedAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ComparedLineItem(
    comparedLine: ComparedLine,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = comparedLine.productName,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "Артикул: ${comparedLine.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "План: ${comparedLine.planQuantity}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Факт: ${comparedLine.factQuantity}",
                style = MaterialTheme.typography.bodyMedium
            )

            val progress = if (comparedLine.planQuantity > 0)
                (comparedLine.factQuantity / comparedLine.planQuantity * 100).toInt()
            else 0

            Text(
                text = "Прогресс: $progress%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class ComparedLine(
    val productId: String,
    val productName: String,
    val articleNumber: String,
    val planQuantity: Float,
    val factQuantity: Float
)

// Функция объединения строк плана и факта
fun compareLines(
    planLines: List<PlanLineX>,
    factLines: List<FactLineX>
): List<ComparedLine> {
    val result = mutableMapOf<String, ComparedLine>()

    // Собираем данные из плана
    planLines.forEach { planLine ->
        planLine.storageProduct?.let { product ->
            val productId = product.product.id
            result[productId] = ComparedLine(
                productId = productId,
                productName = product.product.name,
                articleNumber = product.product.articleNumber,
                planQuantity = product.quantity,
                factQuantity = 0f
            )
        }
    }

    // Добавляем данные из факта
    factLines.forEach { factLine ->
        factLine.storageProduct?.let { product ->
            val productId = product.product.id
            val existing = result[productId]

            if (existing != null) {
                // Обновляем существующую строку
                result[productId] = existing.copy(
                    factQuantity = existing.factQuantity + product.quantity
                )
            } else {
                // Создаем новую строку (если товар есть только в факте)
                result[productId] = ComparedLine(
                    productId = productId,
                    productName = product.product.name,
                    articleNumber = product.product.articleNumber,
                    planQuantity = 0f,
                    factQuantity = product.quantity
                )
            }
        }
    }

    return result.values.toList()
}