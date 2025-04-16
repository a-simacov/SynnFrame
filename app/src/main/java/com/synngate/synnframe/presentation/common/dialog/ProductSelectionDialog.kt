package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import kotlinx.coroutines.delay

@Composable
fun ProductSelectionDialog(
    products: List<Product>,
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit,
    initialFilter: String = "",
    isLoading: Boolean = false,
    title: String = stringResource(id = R.string.select_product),
    planProductIds: Set<String>? = null,
    onFilterChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(initialFilter) }
    val focusRequester = remember { FocusRequester() }

    // Запрашиваем фокус при открытии диалога
    LaunchedEffect(Unit) {
        delay(100) // Небольшая задержка для правильной работы
        focusRequester.requestFocus()
    }

    // Фильтруем продукты по плану, если нужно
    val displayProducts = remember(products, planProductIds) {
        if (planProductIds != null) {
            products.filter { product -> planProductIds.contains(product.id) }
        } else {
            products
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Заголовок диалога
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле поиска по имени, артикулу или штрихкоду
                OutlinedTextField(
                    value = filter,
                    onValueChange = { newValue ->
                        filter = newValue
                        onFilterChanged?.invoke(newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(id = R.string.search_by_name_article_barcode)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (filter.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    filter = ""
                                    onFilterChanged?.invoke("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.search_product_hint)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Список товаров
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_products_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(displayProducts) { product ->
                            ProductItem(
                                product = product,
                                onClick = { onProductSelected(product) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }
}

/**
 * Элемент списка товаров
 */
@Composable
private fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.product_id_fmt, product.id),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.article_fmt, product.articleNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Отображаем штрихкоды и единицы измерения
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Отображаем основной штрихкод, если есть
            val mainUnit = product.getMainUnit()
            if (mainUnit != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Отображаем единицу измерения
                    Text(
                        text = stringResource(id = R.string.unit_fmt, mainUnit.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Отображаем основной штрихкод
                    if (mainUnit.mainBarcode.isNotEmpty()) {
                        Card(
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = mainUnit.mainBarcode,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}