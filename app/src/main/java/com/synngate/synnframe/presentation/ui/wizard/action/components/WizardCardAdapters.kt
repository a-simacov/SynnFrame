package com.synngate.synnframe.presentation.ui.wizard.action.components.adapters

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.wizard.action.components.CardProperty
import com.synngate.synnframe.presentation.ui.wizard.action.components.CardSpacer
import com.synngate.synnframe.presentation.ui.wizard.action.components.WizardCard
import com.synngate.synnframe.presentation.util.formatDate

/**
 * Адаптер для отображения продукта в стандартной карточке
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    WizardCard(
        title = product.name,
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        // Добавляем свойства продукта
        CardProperty(label = "Артикул", value = product.articleNumber)
        CardSpacer()

        product.getMainUnit()?.let { unit ->
            CardProperty(label = "Основная ЕИ", value = unit.name)

            if (unit.mainBarcode.isNotEmpty()) {
                CardSpacer()
                CardProperty(label = "Штрихкод", value = unit.mainBarcode)
            }
        }
    }
}

/**
 * Адаптер для отображения TaskProduct в стандартной карточке
 */
@Composable
fun TaskProductCard(
    taskProduct: TaskProduct,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    WizardCard(
        title = taskProduct.product.name,
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        // Добавляем свойства TaskProduct
        CardProperty(label = "Артикул", value = taskProduct.product.articleNumber)
        CardSpacer()

        // Штрихкод основной единицы измерения
        taskProduct.product.getMainUnit()?.let { unit ->
            if (unit.mainBarcode.isNotEmpty()) {
                CardProperty(label = "Штрихкод", value = unit.mainBarcode)
                CardSpacer()
            }
        }

        // Количество
        if (taskProduct.quantity > 0) {
            CardProperty(
                label = "Количество",
                value = taskProduct.quantity.toString()
            )
            CardSpacer()
        }

        // Статус продукта
        CardProperty(
            label = "Статус",
            value = when (taskProduct.status) {
                ProductStatus.STANDARD -> "Стандартный"
                ProductStatus.EXPIRED -> "Просроченный"
                ProductStatus.DEFECTIVE -> "Брак"
            }
        )

        // Срок годности
        if (taskProduct.hasExpirationDate()) {
            CardSpacer()
            CardProperty(
                label = "Срок годности",
                value = formatDate(taskProduct.expirationDate)
            )
        }
    }
}

@Composable
fun TaskProductCardShort(
    taskProduct: TaskProduct,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    WizardCard(
        title = taskProduct.product.name,
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        CardProperty(
            label = "Статус",
            value = when (taskProduct.status) {
                ProductStatus.STANDARD -> "Стандартный"
                ProductStatus.EXPIRED -> "Просроченный"
                ProductStatus.DEFECTIVE -> "Брак"
            }
        )

        if (taskProduct.hasExpirationDate()) {
            CardSpacer()
            CardProperty(
                label = "Срок годности",
                value = formatDate(taskProduct.expirationDate)
            )
        }
    }
}

/**
 * Адаптер для отображения паллеты в стандартной карточке
 */
@Composable
fun PalletCard(
    pallet: Pallet,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    WizardCard(
        title = "Паллета: ${pallet.code}",
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        // Дополнительная информация о паллете, если есть
        if (pallet.isClosed) {
            CardProperty(label = "Статус", value = "Закрыта")
        }

        // Тут можно добавить другие свойства паллеты
    }
}

/**
 * Адаптер для отображения ячейки в стандартной карточке
 */
@Composable
fun BinCard(
    bin: BinX,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    WizardCard(
        title = "Ячейка: ${bin.code}",
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        // Дополнительная информация о ячейке, если есть
        if (bin.zone.isNotEmpty()) {
            CardProperty(label = "Зона", value = bin.zone)
        }

        // Тут можно добавить другие свойства ячейки
    }
}