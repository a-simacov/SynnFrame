package com.synngate.synnframe.presentation.ui.taskx.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRuleItem
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.theme.SynnFrameTheme
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import java.time.LocalDateTime

@Composable
fun PlannedActionsView(
    plannedActions: List<PlannedAction>,
    onActionClick: (PlannedAction) -> Unit,
    nextActionId: String? = null, // Добавлен параметр для идентификации следующего действия
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (plannedActions.isEmpty()) {
            EmptyScreenContent(message = "Нет запланированных действий")
        } else {
            LazyColumn {
                items(plannedActions.sortedBy { it.order }) { action ->
                    PlannedActionItem(
                        action = action,
                        onClick = { onActionClick(action) },
                        isNextAction = action.id == nextActionId // Передаем флаг для выделения
                    )
                }
            }
        }
    }
}

@Composable
fun FactActionsView(
    factActions: List<FactAction>,
    formatDate: (LocalDateTime) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (factActions.isEmpty()) {
            EmptyScreenContent(message = "Нет выполненных действий")
        } else {
            LazyColumn {
                items(factActions.sortedByDescending { it.completedAt }) { action ->
                    FactActionItem(
                        action = action,
                        formatDate = formatDate
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedActionItem(
    action: PlannedAction,
    onClick: () -> Unit,
    isNextAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        action.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
        action.isSkipped -> MaterialTheme.colorScheme.tertiaryContainer
        isNextAction -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    // Определяем границу карточки - только для следующего действия
    val border = if (isNextAction) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = action.isClickable(), onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                                shape = MaterialTheme.shapes.small
//                            )
//                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isNextAction) {
                            IconWithTooltip(
                                icon = Icons.Default.ArrowForward,
                                description = "Следующее действие",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = action.actionTemplate.name,
                            style = MaterialTheme.typography.titleMedium,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                // Иконка статуса с подсказкой
                when {
                    action.isCompleted -> {
                        IconWithTooltip(
                            icon = Icons.Default.CheckCircle,
                            description = "Выполнено",
                            tint = MaterialTheme.colorScheme.secondary,
                            iconSize = 24
                        )
                    }
                    action.isSkipped -> {
                        IconWithTooltip(
                            icon = Icons.Default.NoEncryption,
                            description = "Пропущено",
                            tint = MaterialTheme.colorScheme.tertiary,
                            iconSize = 24
                        )
                    }
                    else -> {
                        IconWithTooltip(
                            icon = Icons.Default.PendingActions,
                            description = "Ожидает выполнения",
                            tint = MaterialTheme.colorScheme.primary,
                            iconSize = 24
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Отображение товара, если есть
            action.storageProduct?.let { product ->
                Text(
                    text = "Товар: ${product.product.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Отображение паллеты хранения, если есть
            action.storagePallet?.let { pallet ->
                Text(
                    text = "Паллета хранения: ${pallet.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                WmsActionIconWithTooltip(
                    wmsAction = action.wmsAction,
                    iconSize = 18
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getWmsActionDescription(action.wmsAction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Отображение ячейки размещения, если есть
            action.placementBin?.let { bin ->
                Text(
                    text = "Ячейка размещения: ${bin.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Отображение паллеты размещения, если есть
            action.placementPallet?.let { pallet ->
                Text(
                    text = "Паллета размещения: ${pallet.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlannedActionItemPreview() {

    val fromPlanValidationRule = ValidationRule(
        name = "Из плана",
        rules = listOf(
            ValidationRuleItem(
                type = ValidationType.FROM_PLAN,
                errorMessage = "Выберите объект из плана"
            ),
            ValidationRuleItem(
                type = ValidationType.NOT_EMPTY,
                errorMessage = "Объект не должен быть пустым"
            )
        )
    )

    val takePalletTemplate = ActionTemplate(
        id = "template_take_pallet",
        name = "Взять определенную паллету из определенной ячейки",
        wmsAction = WmsAction.TAKE_FROM,
        storageObjectType = ActionObjectType.PALLET,
        placementObjectType = ActionObjectType.BIN,
        storageSteps = listOf(
            ActionStep(
                id = "step_select_planned_pallet",
                order = 1,
                name = "Выберите паллету из запланированного действия",
                promptText = "Выберите паллету из запланированного действия",
                objectType = ActionObjectType.PALLET,
                validationRules = fromPlanValidationRule,
                isRequired = true,
                canSkip = false
            )
        ),
        placementSteps = listOf(
            ActionStep(
                id = "step_select_planned_bin",
                order = 2,
                name = "Выберите ячейку из запланированного действия",
                promptText = "Выберите ячейку из запланированного действия",
                objectType = ActionObjectType.BIN,
                validationRules = fromPlanValidationRule,
                isRequired = true,
                canSkip = false
            )
        )
    )

    SynnFrameTheme {
        PlannedActionItem(
            action = PlannedAction(
                id = "action1",
                order = 1,
                actionTemplate = takePalletTemplate,
                storagePallet = Pallet(code = "IN00000000003", isClosed = false),
                wmsAction = WmsAction.TAKE_FROM,
                placementBin = BinX(
                    code = "B00211",
                    zone = "Хранение",
                    line = "B",
                    rack = "02",
                    tier = "1",
                    position = "1"
                )
            ),
            onClick = { /*TODO*/ },
            isNextAction = true
        )
    }

}

@Composable
fun FactActionItem(
    action: FactAction,
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
                .padding(16.dp)
        ) {
            // Заголовок с иконкой типа действия и подсказкой
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                WmsActionIconWithTooltip(
                    wmsAction = action.wmsAction,
                    iconSize = 24
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = getWmsActionDescription(action.wmsAction),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(4.dp))

            action.storageProduct?.let { ShowStorageProduct(it) }
            action.storagePallet?.let { ShowPallet("Паллета хранения", it) }
            action.placementBin?.let { ShowBin(it) }
            action.placementPallet?.let { ShowPallet("Паллета размещения", it) }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Начато: ${formatDate(action.startedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Завершено: ${formatDate(action.completedAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ShowStorageProduct(product: TaskProduct) {
    Text(
        text = "Товар: ${product.product.name}",
        style = MaterialTheme.typography.bodyMedium
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

@Composable
private fun ShowPallet(title: String, pallet: Pallet) {
    Text(
        text = "$title: ${pallet.code}",
        style = MaterialTheme.typography.bodyMedium
    )

    Text(
        text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ShowBin(bin: BinX) {
    Text(
        text = "Ячейка: ${bin.code}",
        style = MaterialTheme.typography.bodyMedium
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